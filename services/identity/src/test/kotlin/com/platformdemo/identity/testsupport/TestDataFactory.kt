package com.platformdemo.identity.testsupport

import com.platformdemo.identity.auth.endpoint.request.LoginRequest
import com.platformdemo.identity.endpoint.request.RegisterUserRequest
import java.util.UUID

object TestDataFactory {

    fun registerUserRequest(
        email: String = uniqueEmail("register-user"),
        password: String = "S3curePassw0rd!",
        displayName: String = "Test User"
    ): RegisterUserRequest {
        return RegisterUserRequest(
            email = email,
            password = password,
            displayName = displayName
        )
    }

    fun loginRequest(
        email: String,
        password: String = "S3curePassw0rd!"
    ): LoginRequest {
        return LoginRequest(
            email = email,
            password = password
        )
    }

    fun uniqueEmail(prefix: String): String {
        val token = UUID.randomUUID().toString().replace("-", "")
        return "$prefix-$token@example.com"
    }
}
