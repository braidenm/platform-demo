package com.platformdemo.identity.auth.service.provider

import com.platformdemo.identity.auth.service.port.AuthSessionStore
import com.platformdemo.identity.auth.service.port.NewAuthSession
import com.platformdemo.identity.auth.session.AuthSessionRecord
import com.platformdemo.identity.auth.session.AuthSessionRepository
import org.springframework.stereotype.Component

@Component
class PostgresAuthSessionStore(
    private val authSessionRepository: AuthSessionRepository
) : AuthSessionStore {

    override fun create(session: NewAuthSession) {
        authSessionRepository.save(
            AuthSessionRecord(
                sessionId = session.sessionId,
                userId = session.userId,
                provider = session.provider,
                refreshTokenHash = session.refreshTokenHash,
                createdAt = session.createdAt,
                updatedAt = session.updatedAt,
                expiresAt = session.expiresAt,
                revokedAt = session.revokedAt
            )
        )
    }
}
