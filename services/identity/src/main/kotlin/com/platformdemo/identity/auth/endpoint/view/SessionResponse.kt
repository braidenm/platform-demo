package com.platformdemo.identity.auth.endpoint.view

import com.platformdemo.identity.endpoint.view.UserResponse

data class SessionResponse(
    val sessionId: String,
    val user: UserResponse,
    val provider: String,
    val sessionStatus: String,
    val accessTokenExpiresIn: Int,
    val refreshTokenExpiresIn: Int?
)
