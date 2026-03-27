package com.platformdemo.identity.auth.api

import com.platformdemo.identity.auth.application.CommandStatusService
import com.platformdemo.identity.auth.application.RegisterUserCommandService
import com.platformdemo.identity.auth.infrastructure.CommandErrorDocument
import com.platformdemo.identity.auth.infrastructure.CommandStatusDocument
import com.platformdemo.identity.auth.infrastructure.ProducedEventRefDocument
import com.platformdemo.identity.auth.infrastructure.UserReadModelDocument
import com.platformdemo.identity.auth.infrastructure.UserReadModelRepository
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/identity/v1/commands")
class RegisterUserCommandController(
    private val registerUserCommandService: RegisterUserCommandService,
    private val commandStatusService: CommandStatusService
) {

    @PostMapping("/register-user")
    fun registerUser(
        @Valid @RequestBody request: RegisterUserCommandRequest,
        @RequestHeader(name = "Idempotency-Key", required = false) idempotencyKey: String?,
        @RequestHeader(name = "X-Correlation-Id", required = false) correlationId: String?
    ): ResponseEntity<CommandAcceptedResponse> {
        val response = registerUserCommandService.submit(request, idempotencyKey, correlationId)
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response)
    }

    @GetMapping("/{commandId}")
    fun getCommandStatus(@PathVariable commandId: String): CommandStatusResponse {
        return commandStatusService.getOrThrow(commandId).toApiResponse()
    }
}

@RestController
@RequestMapping("/identity/v1/users")
class UserReadModelController(
    private val userReadModelRepository: UserReadModelRepository
) {

    @GetMapping("/{userId}")
    fun getUser(@PathVariable userId: String): UserResponse {
        val user = userReadModelRepository.findById(userId).orElseThrow {
            NotFoundException(
                message = "User not found",
                details = mapOf("user_id" to userId)
            )
        }
        return user.toApiResponse()
    }
}

private fun CommandStatusDocument.toApiResponse(): CommandStatusResponse {
    return CommandStatusResponse(
        commandId = commandId,
        commandType = commandType,
        status = status.name,
        submittedAt = submittedAt,
        completedAt = completedAt,
        correlationId = correlationId,
        error = error?.toApiResponse(),
        producedEvents = producedEvents.map { it.toApiResponse() }
    )
}

private fun CommandErrorDocument.toApiResponse(): CommandErrorResponse {
    return CommandErrorResponse(
        code = code,
        message = message,
        details = details
    )
}

private fun ProducedEventRefDocument.toApiResponse(): ProducedEventRefResponse {
    return ProducedEventRefResponse(
        eventId = eventId,
        eventType = eventType,
        occurredAt = occurredAt
    )
}

private fun UserReadModelDocument.toApiResponse(): UserResponse {
    return UserResponse(
        id = id,
        email = email,
        status = status.name,
        emailVerified = emailVerified,
        createdAt = createdAt
    )
}
