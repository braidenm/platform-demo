package com.platformdemo.identity.auth.api

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class RegisterUserCommandRequest(
    @field:NotBlank
    @field:Email
    val email: String,
    @field:NotBlank
    @field:Size(min = 8, max = 128)
    val password: String,
    @field:Size(min = 1, max = 120)
    val displayName: String? = null
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class CommandAcceptedResponse(
    val commandId: String,
    val commandType: String,
    val status: String,
    val acceptedAt: Instant,
    val correlationId: String? = null,
    val idempotencyKey: String? = null
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class CommandStatusResponse(
    val commandId: String,
    val commandType: String,
    val status: String,
    val submittedAt: Instant,
    val completedAt: Instant? = null,
    val correlationId: String? = null,
    val error: CommandErrorResponse? = null,
    val producedEvents: List<ProducedEventRefResponse> = emptyList()
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class CommandErrorResponse(
    val code: String,
    val message: String,
    val details: Map<String, Any?> = emptyMap()
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class ProducedEventRefResponse(
    val eventId: String,
    val eventType: String,
    val occurredAt: Instant
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class UserResponse(
    val id: String,
    val email: String,
    val status: String,
    val emailVerified: Boolean,
    val createdAt: Instant
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class ErrorEnvelope(
    val error: ApiError
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class ApiError(
    val code: String,
    val message: String,
    val requestId: String,
    val details: Map<String, Any?> = emptyMap()
)
