package com.platformdemo.identity.auth.service.port

data class AuthenticatedUser(
    val userId: String,
    val email: String,
    val status: String
)

fun interface CredentialAuthenticator {
    fun authenticate(email: String, password: String): AuthenticatedUser?
}
