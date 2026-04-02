package com.platformdemo.identity.testsupport

import com.platformdemo.identity.model.RegisterUserRequest

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
        return "$prefix-${System.currentTimeMillis()}@example.com"
    }
}
