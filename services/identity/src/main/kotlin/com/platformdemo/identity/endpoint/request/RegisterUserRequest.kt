package com.platformdemo.identity.endpoint.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

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
