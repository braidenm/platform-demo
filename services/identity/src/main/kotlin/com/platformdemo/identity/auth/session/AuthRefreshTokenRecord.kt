package com.platformdemo.identity.auth.session

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "identity_auth_refresh_tokens",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_identity_auth_refresh_tokens_hash",
            columnNames = ["refresh_token_hash"]
        )
    ],
    indexes = [
        Index(name = "idx_identity_auth_refresh_tokens_session_id", columnList = "session_id"),
        Index(name = "idx_identity_auth_refresh_tokens_user_id", columnList = "user_id"),
        Index(name = "idx_identity_auth_refresh_tokens_expires_at", columnList = "expires_at")
    ]
)
class AuthRefreshTokenRecord(
    @Id
    @Column(name = "token_id", nullable = false, updatable = false, length = 64)
    val tokenId: String,
    @Column(name = "session_id", nullable = false, updatable = false, length = 64)
    val sessionId: String,
    @Column(name = "user_id", nullable = false, updatable = false, length = 64)
    val userId: String,
    @Column(name = "refresh_token_hash", nullable = false, length = 64)
    val refreshTokenHash: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: RefreshTokenStatus,
    @Column(name = "issued_at", nullable = false, updatable = false)
    val issuedAt: Instant,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,
    @Column(name = "consumed_at")
    var consumedAt: Instant? = null,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant
)
