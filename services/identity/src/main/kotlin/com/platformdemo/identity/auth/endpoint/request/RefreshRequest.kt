package com.platformdemo.identity.auth.endpoint.request

import jakarta.validation.constraints.NotBlank

data class RefreshRequest(
    @field:NotBlank
    val refreshToken: String
)
