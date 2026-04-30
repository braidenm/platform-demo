package com.platformdemo.identity.auth.service.model

data class LoginCommand(
    val email: String,
    val password: String
)

data class RefreshCommand(
    val refreshToken: String
)

data class LogoutCommand(
    val refreshToken: String
)

data class LoginResult(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Int,
    val refreshToken: String,
    val refreshExpiresIn: Int,
    val scope: String? = null
)

enum class SessionStatus {
    ACTIVE,
    EXPIRED,
    REVOKED
}

data class SessionResult(
    val sessionId: String,
    val userId: String,
    val provider: String,
    val status: SessionStatus,
    val accessTokenExpiresIn: Int,
    val refreshTokenExpiresIn: Int?
)
