package com.platformdemo.identity.endpoint.view

import java.time.Instant

data class UserRegistrationResponse(
    val userId: String,
    val email: String,
    val status: String,
    val createdAt: Instant
)

data class UserResponse(
    val id: String,
    val email: String,
    val status: String,
    val emailVerified: Boolean,
    val createdAt: Instant
)
