package com.platformdemo.identity.auth.service

import com.platformdemo.identity.auth.service.model.LoginCommand
import com.platformdemo.identity.auth.service.model.LoginResult
import com.platformdemo.identity.auth.service.port.AuthSessionStore
import com.platformdemo.identity.auth.service.port.CredentialAuthenticator
import com.platformdemo.identity.auth.service.port.NewAuthSession
import com.platformdemo.identity.auth.service.port.SessionTokenIssuer
import com.platformdemo.identity.handler.UnauthorizedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class LoginService(
    private val credentialAuthenticator: CredentialAuthenticator,
    private val sessionTokenIssuer: SessionTokenIssuer,
    private val authSessionStore: AuthSessionStore,
    private val clock: Clock
) {

    @Transactional
    fun login(command: LoginCommand): LoginResult {
        val authenticatedUser = credentialAuthenticator.authenticate(command.email, command.password)
            ?: throw UnauthorizedException("Invalid email or password")

        val issuedAt = Instant.now(clock)
        val sessionId = newPrefixedId("ses")
        val issuedTokens = sessionTokenIssuer.issue(
            userId = authenticatedUser.userId,
            sessionId = sessionId,
            provider = authenticatedUser.provider,
            issuedAt = issuedAt
        )

        authSessionStore.create(
            NewAuthSession(
                sessionId = sessionId,
                userId = authenticatedUser.userId,
                provider = authenticatedUser.provider,
                refreshTokenHash = issuedTokens.refreshTokenHash,
                createdAt = issuedAt,
                updatedAt = issuedAt,
                expiresAt = issuedTokens.refreshTokenExpiresAt
            )
        )

        return LoginResult(
            accessToken = issuedTokens.accessToken,
            tokenType = TOKEN_TYPE_BEARER,
            expiresIn = Duration.between(issuedAt, issuedTokens.accessTokenExpiresAt).seconds.toInt(),
            refreshToken = issuedTokens.refreshToken,
            refreshExpiresIn = Duration.between(issuedAt, issuedTokens.refreshTokenExpiresAt).seconds.toInt()
        )
    }

    private fun newPrefixedId(prefix: String): String {
        return "${prefix}_${UUID.randomUUID().toString().replace("-", "")}"
    }

    companion object {
        private const val TOKEN_TYPE_BEARER = "Bearer"
    }
}
