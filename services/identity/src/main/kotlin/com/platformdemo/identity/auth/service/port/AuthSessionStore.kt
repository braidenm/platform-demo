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

fun interface AuthSessionStore {
    fun create(session: NewAuthSession)
}
