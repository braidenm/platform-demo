package com.platformdemo.identity.auth.service.port

import java.time.Instant

data class IssuedSessionTokens(
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
    val refreshToken: String,
    val refreshTokenHash: String,
    val refreshTokenExpiresAt: Instant
)

fun interface SessionTokenIssuer {
    fun issue(userId: String, issuedAt: Instant): IssuedSessionTokens
}
