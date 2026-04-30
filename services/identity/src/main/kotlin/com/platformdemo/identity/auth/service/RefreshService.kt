package com.platformdemo.identity.auth.service

import com.platformdemo.identity.auth.service.model.LoginResult
import com.platformdemo.identity.auth.service.model.RefreshCommand
import com.platformdemo.identity.auth.service.port.AuthSessionStore
import com.platformdemo.identity.auth.service.port.RefreshTokenHasher
import com.platformdemo.identity.auth.service.port.SessionTokenIssuer
import com.platformdemo.identity.auth.service.port.StoredRefreshTokenState
import com.platformdemo.identity.handler.UnauthorizedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.Instant

@Service
class RefreshService(
    private val sessionTokenIssuer: SessionTokenIssuer,
    private val authSessionStore: AuthSessionStore,
    private val refreshTokenHasher: RefreshTokenHasher,
    private val clock: Clock
) {

    @Transactional
    fun refresh(command: RefreshCommand): LoginResult {
        val now = Instant.now(clock)
        val refreshTokenHash = refreshTokenHasher.hash(command.refreshToken)

        val storedRefreshToken = authSessionStore.findRefreshTokenByHash(refreshTokenHash)
            ?: throw UnauthorizedException(INVALID_REFRESH_TOKEN_MESSAGE)

        if (storedRefreshToken.refreshTokenState == StoredRefreshTokenState.ROTATED) {
            authSessionStore.revokeSession(storedRefreshToken.sessionId, now)
            throw UnauthorizedException(REFRESH_TOKEN_REUSE_MESSAGE)
        }

        if (storedRefreshToken.refreshTokenState == StoredRefreshTokenState.REVOKED) {
            throw UnauthorizedException(INVALID_REFRESH_TOKEN_MESSAGE)
        }

        if (storedRefreshToken.sessionRevokedAt != null ||
            storedRefreshToken.sessionExpiresAt <= now ||
            storedRefreshToken.refreshTokenExpiresAt <= now
        ) {
            authSessionStore.revokeSession(storedRefreshToken.sessionId, now)
            throw UnauthorizedException(INVALID_REFRESH_TOKEN_MESSAGE)
        }

        val issuedTokens = sessionTokenIssuer.issue(
            userId = storedRefreshToken.userId,
            sessionId = storedRefreshToken.sessionId,
            provider = storedRefreshToken.provider,
            issuedAt = now
        )
        val rotated = authSessionStore.rotateRefreshToken(
            sessionId = storedRefreshToken.sessionId,
            currentRefreshTokenHash = storedRefreshToken.refreshTokenHash,
            newRefreshTokenHash = issuedTokens.refreshTokenHash,
            newRefreshTokenExpiresAt = issuedTokens.refreshTokenExpiresAt,
            rotatedAt = now
        )
        if (!rotated) {
            authSessionStore.revokeSession(storedRefreshToken.sessionId, now)
            throw UnauthorizedException(REFRESH_TOKEN_REUSE_MESSAGE)
        }

        return LoginResult(
            accessToken = issuedTokens.accessToken,
            tokenType = TOKEN_TYPE_BEARER,
            expiresIn = Duration.between(now, issuedTokens.accessTokenExpiresAt).seconds.toInt(),
            refreshToken = issuedTokens.refreshToken,
            refreshExpiresIn = Duration.between(now, issuedTokens.refreshTokenExpiresAt).seconds.toInt()
        )
    }

    companion object {
        private const val TOKEN_TYPE_BEARER = "Bearer"
        private const val INVALID_REFRESH_TOKEN_MESSAGE = "Invalid refresh token"
        private const val REFRESH_TOKEN_REUSE_MESSAGE = "Refresh token reuse detected; please log in again"
    }
}
