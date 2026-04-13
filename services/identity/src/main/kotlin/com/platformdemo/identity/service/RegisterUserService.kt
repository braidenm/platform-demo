package com.platformdemo.identity.service

import com.platformdemo.identity.command.RegisterUserCommand
import com.platformdemo.identity.handler.BadRequestException
import com.platformdemo.identity.handler.ConflictException
import com.platformdemo.identity.endpoint.request.RegisterUserRequest
import com.platformdemo.identity.endpoint.view.UserRegistrationResponse
import com.platformdemo.identity.repository.postgres.UserCredentialRepository
import com.platformdemo.shared.idempotency.IdempotencySupport
import jakarta.validation.ValidationException
import org.axonframework.commandhandling.GenericCommandMessage
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.util.Locale
import java.util.UUID

@Service
class RegisterUserService(
    private val commandGateway: CommandGateway,
    private val passwordEncoder: PasswordEncoder,
    private val userCredentialRepository: UserCredentialRepository,
    private val idempotencySupport: IdempotencySupport,
    private val clock: Clock = Clock.systemUTC()
) {

    fun register(request: RegisterUserRequest, idempotencyKeyHeader: String?): UserRegistrationResponse {
        val idempotencyKey = try {
            idempotencySupport.normalize(idempotencyKeyHeader)
        } catch (ex: ValidationException) {
            throw BadRequestException(ex.message ?: "Invalid Idempotency-Key")
        }

        if (idempotencyKey != null) {
            val existingRecord = idempotencySupport.find(SCOPE, idempotencyKey)
            if (existingRecord != null) {
                return UserRegistrationResponse(
                    userId = existingRecord.resourceId,
                    email = request.email.trim().lowercase(Locale.US),
                    status = "ACTIVE",
                    createdAt = existingRecord.createdAt
                )
            }
        }

        val createdAt = Instant.now(clock)
        val userId = newPrefixedId("usr")
        val email = request.email.trim().lowercase(Locale.US)
        val displayName = request.displayName?.trim()?.takeIf { it.isNotEmpty() }

        val existingCredential = userCredentialRepository.findByEmail(email)
        if (existingCredential != null) {
            throw ConflictException("Email is already registered", details = mapOf("email" to email))
        }

        val commandMessage = GenericCommandMessage.asCommandMessage<RegisterUserCommand>(
            RegisterUserCommand(
                userId = userId,
                email = email,
                displayName = displayName,
                passwordHash = passwordEncoder.encode(request.password),
                registeredAt = createdAt,
                idempotencyKey = idempotencyKey
            )
        )
        commandGateway.sendAndWait<Any>(commandMessage)

        return UserRegistrationResponse(
            userId = userId,
            email = email,
            status = "ACTIVE",
            createdAt = createdAt
        )
    }

    private fun newPrefixedId(prefix: String): String {
        return "${prefix}_${UUID.randomUUID().toString().replace("-", "")}"
    }

    companion object {
        private const val SCOPE = "identity.register-user"
    }
}
