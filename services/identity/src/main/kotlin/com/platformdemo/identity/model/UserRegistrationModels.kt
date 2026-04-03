package com.platformdemo.identity.model

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class RegisterUserRequest(
    @field:NotBlank
    @field:Email
    val email: String,
    @field:NotBlank
    @field:Size(min = 8, max = 128)
    val password: String,
    @field:Size(min = 1, max = 120)
    val displayName: String? = null
)

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
