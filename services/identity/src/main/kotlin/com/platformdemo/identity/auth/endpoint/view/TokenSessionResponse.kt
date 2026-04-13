package com.platformdemo.identity.auth.endpoint.view

data class TokenSessionResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Int,
    val refreshToken: String,
    val refreshExpiresIn: Int,
    val scope: String? = null
)
