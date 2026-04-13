package com.platformdemo.identity.event

import java.time.Instant

data class UserRegisteredEvent(
    val userId: String,
    val email: String,
    val displayName: String?,
    val passwordHash: String? = null,
    val registeredAt: Instant,
    val status: UserStatus,
    val idempotencyKey: String? = null
)
