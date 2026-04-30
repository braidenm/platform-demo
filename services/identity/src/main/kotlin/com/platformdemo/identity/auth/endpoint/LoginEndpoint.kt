package com.platformdemo.identity.auth.endpoint

import com.platformdemo.identity.auth.endpoint.request.LoginRequest
import com.platformdemo.identity.auth.endpoint.view.TokenSessionResponse
import com.platformdemo.identity.auth.service.LoginService
import com.platformdemo.identity.auth.service.model.LoginCommand
import com.platformdemo.identity.auth.service.model.LoginResult
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1")
class LoginEndpoint(
    private val loginService: LoginService
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): TokenSessionResponse {
        val result = loginService.login(
            LoginCommand(
                email = request.email,
                password = request.password
            )
        )
        return result.toResponse()
    }
}

private fun LoginResult.toResponse(): TokenSessionResponse {
    return TokenSessionResponse(
        accessToken = accessToken,
        tokenType = tokenType,
        expiresIn = expiresIn,
        refreshToken = refreshToken,
        refreshExpiresIn = refreshExpiresIn,
        scope = scope
    )
}
