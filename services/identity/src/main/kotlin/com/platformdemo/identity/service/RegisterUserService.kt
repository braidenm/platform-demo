package com.platformdemo.identity.service

import com.platformdemo.identity.handler.BadRequestException
import com.platformdemo.identity.handler.ConflictException
import com.platformdemo.identity.model.RegisterUserRequest
import com.platformdemo.identity.model.UserRegistrationResponse
import com.platformdemo.identity.command.RegisterUserCommand
import com.platformdemo.identity.event.UserStatus
import com.platformdemo.identity.repository.StoredUserCredential
import com.platformdemo.identity.repository.UserCredentialRepository
import com.platformdemo.identity.view.UserProjectionRepository
import com.platformdemo.shared.idempotency.IdempotencySupport
import jakarta.validation.ValidationException
import org.axonframework.commandhandling.GenericCommandMessage
import org.axonframework.commandhandling.gateway.CommandGateway
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.Locale
import java.util.UUID

@Service
class RegisterUserService(
    private val commandGateway: CommandGateway,
    private val passwordEncoder: PasswordEncoder,
    private val userCredentialRepository: UserCredentialRepository,
    private val userProjectionRepository: UserProjectionRepository,
    private val idempotencySupport: IdempotencySupport,
    private val clock: Clock = Clock.systemUTC()
) {

    private val logger = LoggerFactory.getLogger(RegisterUserService::class.java)

    @Transactional
    fun register(request: RegisterUserRequest, idempotencyKeyHeader: String?): UserRegistrationResponse {
        val idempotencyKey = try {
            idempotencySupport.normalize(idempotencyKeyHeader)
        } catch (ex: ValidationException) {
            throw BadRequestException(ex.message ?: "Invalid Idempotency-Key")
        }

        if (idempotencyKey != null) {
            val existingRecord = idempotencySupport.find(SCOPE, idempotencyKey)
            if (existingRecord != null) {
                val existingUser = userProjectionRepository.findById(existingRecord.resourceId).orElse(null)
                if (existingUser != null) {
                    return UserRegistrationResponse(
                        userId = existingUser.id,
                        email = existingUser.email,
                        status = existingUser.status.name,
                        createdAt = existingUser.createdAt
                    )
                }

                val existingCredential = userCredentialRepository.findById(existingRecord.resourceId).orElse(null)
                if (existingCredential != null) {
                    return UserRegistrationResponse(
                        userId = existingCredential.userId,
                        email = existingCredential.email,
                        status = existingCredential.status,
                        createdAt = existingCredential.createdAt
                    )
                }
            }
        }

        val createdAt = Instant.now(clock)
        val userId = newPrefixedId("usr")
        val email = normalizeEmail(request.email)
        val displayName = request.displayName?.trim()?.takeIf { it.isNotEmpty() }

        if (userCredentialRepository.existsByEmail(email)) {
            throw ConflictException("Email is already registered", details = mapOf("email" to email))
        }

        try {
            userCredentialRepository.save(
                StoredUserCredential(
                    userId = userId,
                    email = email,
                    passwordHash = passwordEncoder.encode(request.password),
                    status = UserStatus.ACTIVE.name,
                    createdAt = createdAt,
                    updatedAt = createdAt
                )
            )
        } catch (ex: DataIntegrityViolationException) {
            throw ConflictException("Email is already registered", details = mapOf("email" to email))
        }

        try {
            val commandMessage = GenericCommandMessage.asCommandMessage<RegisterUserCommand>(
                RegisterUserCommand(
                    userId = userId,
                    email = email,
                    displayName = displayName,
                    registeredAt = createdAt
                )
            )
            commandGateway.sendAndWait<Any>(commandMessage)
        } catch (ex: Exception) {
            logger.error("Failed to register user. userId={}", userId, ex)
            throw ex
        }

        if (idempotencyKey != null) {
            idempotencySupport.remember(SCOPE, idempotencyKey, userId)
        }

        return UserRegistrationResponse(
            userId = userId,
            email = email,
            status = UserStatus.ACTIVE.name,
            createdAt = createdAt
        )
    }

    private fun normalizeEmail(email: String): String {
        return email.trim().lowercase(Locale.US)
    }

    private fun newPrefixedId(prefix: String): String {
        return "${prefix}_${UUID.randomUUID().toString().replace("-", "")}"
    }

    companion object {
        private const val SCOPE = "identity.register-user"
    }
}
