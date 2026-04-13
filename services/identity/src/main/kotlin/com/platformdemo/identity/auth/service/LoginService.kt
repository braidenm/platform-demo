package com.platformdemo.identity.auth.service

import com.platformdemo.identity.auth.endpoint.request.LoginRequest
import com.platformdemo.identity.auth.endpoint.view.TokenSessionResponse
import com.platformdemo.identity.auth.service.port.CredentialAuthenticator
import com.platformdemo.identity.auth.service.port.SessionTokenIssuer
import com.platformdemo.identity.auth.session.AuthSessionRecord
import com.platformdemo.identity.auth.session.AuthSessionRepository
import com.platformdemo.identity.handler.UnauthorizedException
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class LoginService(
    private val credentialAuthenticator: CredentialAuthenticator,
    private val sessionTokenIssuer: SessionTokenIssuer,
    private val authSessionRepository: AuthSessionRepository,
    private val clock: Clock = Clock.systemUTC()
) {

    fun login(request: LoginRequest): TokenSessionResponse {
        val authenticatedUser = credentialAuthenticator.authenticate(request.email, request.password)
            ?: throw UnauthorizedException("Invalid email or password")

        val issuedAt = Instant.now(clock)
        val issuedTokens = sessionTokenIssuer.issue(authenticatedUser.userId, issuedAt)

        authSessionRepository.save(
            AuthSessionRecord(
                sessionId = newPrefixedId("ses"),
                userId = authenticatedUser.userId,
                provider = LOCAL_PROVIDER,
                refreshTokenHash = issuedTokens.refreshTokenHash,
                createdAt = issuedAt,
                updatedAt = issuedAt,
                expiresAt = issuedTokens.refreshTokenExpiresAt
            )
        )

        return TokenSessionResponse(
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
        private const val LOCAL_PROVIDER = "LOCAL"
        private const val TOKEN_TYPE_BEARER = "Bearer"
    }
}
