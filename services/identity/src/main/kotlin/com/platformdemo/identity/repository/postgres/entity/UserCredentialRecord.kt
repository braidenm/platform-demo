package com.platformdemo.identity.repository.postgres.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "identity_user_credentials",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_identity_user_credentials_email", columnNames = ["email"])
    ]
)
class UserCredentialRecord(
    @Id
    @Column(name = "user_id", nullable = false, updatable = false, length = 64)
    val userId: String,
    @Column(name = "email", nullable = false, length = 320)
    val email: String,
    @Column(name = "password_hash", nullable = false, length = 120)
    val passwordHash: String,
    @Column(name = "status", nullable = false, length = 32)
    val status: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant
)