package com.platformdemo.identity.auth.session

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "identity_auth_sessions",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_identity_auth_sessions_refresh_token_hash",
            columnNames = ["refresh_token_hash"]
        )
    ],
    indexes = [
        Index(name = "idx_identity_auth_sessions_user_id", columnList = "user_id"),
        Index(name = "idx_identity_auth_sessions_expires_at", columnList = "expires_at")
    ]
)
class AuthSessionRecord(
    @Id
    @Column(name = "session_id", nullable = false, updatable = false, length = 64)
    val sessionId: String,
    @Column(name = "user_id", nullable = false, updatable = false, length = 64)
    val userId: String,
    @Column(name = "provider", nullable = false, length = 32)
    val provider: String,
    @Column(name = "refresh_token_hash", nullable = false, length = 64)
    val refreshTokenHash: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,
    @Column(name = "revoked_at")
    var revokedAt: Instant? = null
)
