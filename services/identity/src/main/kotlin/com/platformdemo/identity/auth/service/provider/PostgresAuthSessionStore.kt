package com.platformdemo.identity.auth.service.provider

import com.platformdemo.identity.auth.service.port.AuthSessionStore
import com.platformdemo.identity.auth.service.port.NewAuthSession
import com.platformdemo.identity.auth.service.port.StoredAuthSession
import com.platformdemo.identity.auth.service.port.StoredRefreshToken
import com.platformdemo.identity.auth.service.port.StoredRefreshTokenState
import com.platformdemo.identity.auth.session.AuthRefreshTokenRecord
import com.platformdemo.identity.auth.session.AuthRefreshTokenRepository
import com.platformdemo.identity.auth.session.AuthSessionRecord
import com.platformdemo.identity.auth.session.AuthSessionRepository
import com.platformdemo.identity.auth.session.RefreshTokenStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Component
class PostgresAuthSessionStore(
    private val authSessionRepository: AuthSessionRepository,
    private val authRefreshTokenRepository: AuthRefreshTokenRepository
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
        authRefreshTokenRepository.save(
            AuthRefreshTokenRecord(
                tokenId = newPrefixedId("rtkrec"),
                sessionId = session.sessionId,
                userId = session.userId,
                refreshTokenHash = session.refreshTokenHash,
                status = RefreshTokenStatus.ACTIVE,
                issuedAt = session.createdAt,
                expiresAt = session.expiresAt,
                updatedAt = session.updatedAt
            )
        )
    }

    override fun findRefreshTokenByHash(refreshTokenHash: String): StoredRefreshToken? {
        val refreshTokenRecord = authRefreshTokenRepository.findByRefreshTokenHash(refreshTokenHash) ?: return null
        val sessionRecord = authSessionRepository.findById(refreshTokenRecord.sessionId).orElse(null) ?: return null
        return StoredRefreshToken(
            sessionId = sessionRecord.sessionId,
            userId = sessionRecord.userId,
            provider = sessionRecord.provider,
            refreshTokenHash = refreshTokenRecord.refreshTokenHash,
            refreshTokenState = refreshTokenRecord.status.toStoredState(),
            refreshTokenExpiresAt = refreshTokenRecord.expiresAt,
            sessionExpiresAt = sessionRecord.expiresAt,
            sessionRevokedAt = sessionRecord.revokedAt
        )
    }

    override fun rotateRefreshToken(
        sessionId: String,
        currentRefreshTokenHash: String,
        newRefreshTokenHash: String,
        newRefreshTokenExpiresAt: Instant,
        rotatedAt: Instant
    ): Boolean {
        val currentRefreshToken = authRefreshTokenRepository.findForUpdateByRefreshTokenHash(currentRefreshTokenHash)
            ?: return false
        if (currentRefreshToken.sessionId != sessionId || currentRefreshToken.status != RefreshTokenStatus.ACTIVE) {
            return false
        }

        val sessionRecord = authSessionRepository.findById(sessionId).orElse(null) ?: return false
        if (sessionRecord.revokedAt != null) {
            return false
        }

        currentRefreshToken.status = RefreshTokenStatus.ROTATED
        currentRefreshToken.consumedAt = rotatedAt
        currentRefreshToken.updatedAt = rotatedAt

        authRefreshTokenRepository.save(currentRefreshToken)
        authRefreshTokenRepository.save(
            AuthRefreshTokenRecord(
                tokenId = newPrefixedId("rtkrec"),
                sessionId = sessionRecord.sessionId,
                userId = sessionRecord.userId,
                refreshTokenHash = newRefreshTokenHash,
                status = RefreshTokenStatus.ACTIVE,
                issuedAt = rotatedAt,
                expiresAt = newRefreshTokenExpiresAt,
                updatedAt = rotatedAt
            )
        )

        sessionRecord.refreshTokenHash = newRefreshTokenHash
        sessionRecord.expiresAt = newRefreshTokenExpiresAt
        sessionRecord.updatedAt = rotatedAt
        authSessionRepository.save(sessionRecord)

        return true
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun revokeSession(sessionId: String, revokedAt: Instant) {
        authSessionRepository.findById(sessionId).ifPresent { session ->
            if (session.revokedAt == null) {
                session.revokedAt = revokedAt
                session.updatedAt = revokedAt
                authSessionRepository.save(session)
            }
        }

        val tokenRecords = authRefreshTokenRepository.findBySessionId(sessionId)
        if (tokenRecords.isEmpty()) {
            return
        }
        tokenRecords.forEach { token ->
            if (token.status != RefreshTokenStatus.REVOKED) {
                token.status = RefreshTokenStatus.REVOKED
                token.consumedAt = token.consumedAt ?: revokedAt
                token.updatedAt = revokedAt
            }
        }
        authRefreshTokenRepository.saveAll(tokenRecords)
    }

    override fun findBySessionId(sessionId: String): StoredAuthSession? {
        return authSessionRepository.findById(sessionId).orElse(null)
            ?.let { session ->
                StoredAuthSession(
                    sessionId = session.sessionId,
                    userId = session.userId,
                    provider = session.provider,
                    expiresAt = session.expiresAt,
                    revokedAt = session.revokedAt
                )
            }
    }

    private fun RefreshTokenStatus.toStoredState(): StoredRefreshTokenState {
        return when (this) {
            RefreshTokenStatus.ACTIVE -> StoredRefreshTokenState.ACTIVE
            RefreshTokenStatus.ROTATED -> StoredRefreshTokenState.ROTATED
            RefreshTokenStatus.REVOKED -> StoredRefreshTokenState.REVOKED
        }
    }

    private fun newPrefixedId(prefix: String): String {
        return "${prefix}_${UUID.randomUUID().toString().replace("-", "")}"
    }
}
