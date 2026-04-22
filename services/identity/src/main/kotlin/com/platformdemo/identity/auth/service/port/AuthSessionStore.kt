package com.platformdemo.identity.auth.service.port

import java.time.Instant

data class NewAuthSession(
    val sessionId: String,
    val userId: String,
    val provider: String,
    val refreshTokenHash: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val expiresAt: Instant,
    val revokedAt: Instant? = null
)

enum class StoredRefreshTokenState {
    ACTIVE,
    ROTATED,
    REVOKED
}

data class StoredRefreshToken(
    val sessionId: String,
    val userId: String,
    val provider: String,
    val refreshTokenHash: String,
    val refreshTokenState: StoredRefreshTokenState,
    val refreshTokenExpiresAt: Instant,
    val sessionExpiresAt: Instant,
    val sessionRevokedAt: Instant?
)

data class StoredAuthSession(
    val sessionId: String,
    val userId: String,
    val provider: String,
    val expiresAt: Instant,
    val revokedAt: Instant?
)

interface AuthSessionStore {
    fun create(session: NewAuthSession)
    fun findRefreshTokenByHash(refreshTokenHash: String): StoredRefreshToken?
    fun rotateRefreshToken(
        sessionId: String,
        currentRefreshTokenHash: String,
        newRefreshTokenHash: String,
        newRefreshTokenExpiresAt: Instant,
        rotatedAt: Instant
    ): Boolean
    fun revokeSession(sessionId: String, revokedAt: Instant)
    fun findBySessionId(sessionId: String): StoredAuthSession?
}
