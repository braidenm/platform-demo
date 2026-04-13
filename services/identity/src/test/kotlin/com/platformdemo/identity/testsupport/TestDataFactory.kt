package com.platformdemo.identity.testsupport

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

    fun uniqueEmail(prefix: String): String {
        val token = UUID.randomUUID().toString().replace("-", "")
        return "$prefix-$token@example.com"
    }
}
