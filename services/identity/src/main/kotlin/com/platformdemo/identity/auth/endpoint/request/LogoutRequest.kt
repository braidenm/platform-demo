package com.platformdemo.identity.auth.endpoint.request

import jakarta.validation.constraints.NotBlank

data class LogoutRequest(
    @field:NotBlank
    val refreshToken: String
)
