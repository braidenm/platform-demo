package com.platformdemo.identity.auth.service.model

data class LoginCommand(
    val email: String,
    val password: String
)

data class LoginResult(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Int,
    val refreshToken: String,
    val refreshExpiresIn: Int,
    val scope: String? = null
)
