package com.platformdemo.identity.auth.endpoint

import com.platformdemo.identity.auth.endpoint.request.LoginRequest
import com.platformdemo.identity.auth.endpoint.view.TokenSessionResponse
import com.platformdemo.identity.auth.service.LoginService
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
        return loginService.login(request)
    }
}
