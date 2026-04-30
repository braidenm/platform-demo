package com.platformdemo.identity.auth.service.port

fun interface RefreshTokenHasher {
    fun hash(token: String): String
}
