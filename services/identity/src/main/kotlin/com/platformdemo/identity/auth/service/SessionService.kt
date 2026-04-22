package com.platformdemo.identity.auth.service

import com.platformdemo.identity.auth.service.model.SessionResult
import com.platformdemo.identity.auth.service.model.SessionStatus
import com.platformdemo.identity.auth.service.port.AuthSessionStore
import com.platformdemo.identity.handler.UnauthorizedException
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.Instant

@Service
class SessionService(
    private val authSessionStore: AuthSessionStore,
    private val clock: Clock
) {

    fun getSession(
        userId: String,
        sessionId: String,
        accessTokenExpiresAt: Instant?
    ): SessionResult {
        val now = Instant.now(clock)
        val accessTokenExpiresIn = accessTokenExpiresAt
            ?.let { expiration -> Duration.between(now, expiration).seconds.coerceAtLeast(0).toInt() }
            ?: 0

        val storedSession = authSessionStore.findBySessionId(sessionId)
            ?: throw UnauthorizedException(INVALID_SESSION_MESSAGE)
        if (storedSession.userId != userId) {
            throw UnauthorizedException(INVALID_SESSION_MESSAGE)
        }

        val sessionStatus = when {
            storedSession.revokedAt != null -> SessionStatus.REVOKED
            storedSession.expiresAt <= now -> SessionStatus.EXPIRED
            else -> SessionStatus.ACTIVE
        }
        val refreshTokenExpiresIn = when (sessionStatus) {
            SessionStatus.ACTIVE -> Duration.between(now, storedSession.expiresAt).seconds.coerceAtLeast(0).toInt()
            SessionStatus.EXPIRED, SessionStatus.REVOKED -> null
        }

        return SessionResult(
            sessionId = storedSession.sessionId,
            userId = userId,
            provider = storedSession.provider,
            status = sessionStatus,
            accessTokenExpiresIn = accessTokenExpiresIn,
            refreshTokenExpiresIn = refreshTokenExpiresIn
        )
    }

    companion object {
        private const val INVALID_SESSION_MESSAGE = "Invalid session"
    }
}
