package com.platformdemo.identity.endpoint

import com.platformdemo.identity.handler.NotFoundException
import com.platformdemo.identity.repository.mongo.entity.UserProjection
import com.platformdemo.identity.endpoint.request.RegisterUserRequest
import com.platformdemo.identity.endpoint.view.UserRegistrationResponse
import com.platformdemo.identity.endpoint.view.UserResponse
import com.platformdemo.identity.repository.mongo.UserProjectionRepository
import com.platformdemo.identity.service.RegisterUserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1")
class UserRegistrationEndpoint(
    private val registerUserService: RegisterUserService,
) {

    @PostMapping("/register-user")
    @ResponseStatus(HttpStatus.CREATED)
    fun registerUser(
        @Valid @RequestBody request: RegisterUserRequest,
        @RequestHeader(name = "Idempotency-Key", required = false) idempotencyKey: String?
    ): UserRegistrationResponse {
        return registerUserService.register(request, idempotencyKey)
    }
}

@RestController
@RequestMapping("/v1/users")
class UserViewEndpoint(
    private val userProjectionRepository: UserProjectionRepository
) {

    @GetMapping("/{userId}")
    fun getUser(@PathVariable userId: String): UserResponse {
        val user = userProjectionRepository.findById(userId).orElseThrow {
            NotFoundException(
                message = "User not found",
                details = mapOf("user_id" to userId)
            )
        }
        return user.toApiResponse()
    }
}

private fun UserProjection.toApiResponse(): UserResponse {
    return UserResponse(
        id = id,
        email = email,
        status = status.name,
        emailVerified = emailVerified,
        createdAt = createdAt
    )
}
