package com.platformdemo.identity.auth.service

import com.platformdemo.identity.auth.service.model.LogoutCommand
import com.platformdemo.identity.auth.service.port.AuthSessionStore
import com.platformdemo.identity.auth.service.port.RefreshTokenHasher
import com.platformdemo.identity.handler.UnauthorizedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class LogoutService(
    private val authSessionStore: AuthSessionStore,
    private val refreshTokenHasher: RefreshTokenHasher,
    private val clock: Clock
) {

    @Transactional
    fun logout(command: LogoutCommand) {
        val now = Instant.now(clock)
        val refreshTokenHash = refreshTokenHasher.hash(command.refreshToken)
        val storedRefreshToken = authSessionStore.findRefreshTokenByHash(refreshTokenHash)
            ?: throw UnauthorizedException(INVALID_REFRESH_TOKEN_MESSAGE)

        authSessionStore.revokeSession(storedRefreshToken.sessionId, now)
    }

    companion object {
        private const val INVALID_REFRESH_TOKEN_MESSAGE = "Invalid refresh token"
    }
}
