package com.platformdemo.identity.event

import java.time.Instant

data class UserRegisteredEvent(
    val userId: String,
    val email: String,
    val displayName: String?,
    val registeredAt: Instant,
    val status: UserStatus
)
