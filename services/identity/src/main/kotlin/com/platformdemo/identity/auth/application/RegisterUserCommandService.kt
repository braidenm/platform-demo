package com.platformdemo.identity.auth.application

import com.platformdemo.identity.auth.api.BadRequestException
import com.platformdemo.identity.auth.api.CommandAcceptedResponse
import com.platformdemo.identity.auth.api.ConflictException
import com.platformdemo.identity.auth.api.RegisterUserRequest
import com.platformdemo.identity.auth.domain.RegisterUserCommand
import com.platformdemo.identity.auth.domain.UserStatus
import com.platformdemo.identity.auth.infrastructure.CommandProcessingStatus
import com.platformdemo.identity.auth.infrastructure.UserCredentialEntity
import com.platformdemo.identity.auth.infrastructure.UserCredentialRepository
import org.axonframework.commandhandling.GenericCommandMessage
import org.axonframework.commandhandling.gateway.CommandGateway
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Clock
import java.time.Instant
import java.util.Locale
import java.util.UUID

@Service
class RegisterUserCommandService(
    private val commandGateway: CommandGateway,
    private val passwordEncoder: PasswordEncoder,
    private val userCredentialRepository: UserCredentialRepository,
    private val commandStatusService: CommandStatusService,
    private val clock: Clock = Clock.systemUTC()
) {

    private val logger = LoggerFactory.getLogger(RegisterUserCommandService::class.java)

    @Transactional
    fun submit(
        request: RegisterUserRequest,
        idempotencyKeyHeader: String?,
        correlationIdHeader: String?
    ): CommandAcceptedResponse {
        val idempotencyKey = normalizeIdempotencyKey(idempotencyKeyHeader)
        idempotencyKey?.let { key ->
            commandStatusService.findByIdempotencyKey(key)?.let { existing ->
                return existing.toAcceptedResponse()
            }
        }

        val submittedAt = Instant.now(clock)
        val commandId = newPrefixedId("cmd")
        val userId = newPrefixedId("usr")
        val correlationId = correlationIdHeader?.trim()?.takeIf { it.isNotEmpty() } ?: commandId
        val email = normalizeEmail(request.email)
        val displayName = request.displayName?.trim()?.takeIf { it.isNotEmpty() }

        try {
            commandStatusService.createReceived(
                commandId = commandId,
                commandType = REGISTER_USER_COMMAND_TYPE,
                submittedAt = submittedAt,
                correlationId = correlationId,
                idempotencyKey = idempotencyKey
            )
        } catch (ex: DataIntegrityViolationException) {
            if (idempotencyKey != null) {
                commandStatusService.findByIdempotencyKey(idempotencyKey)?.let { existing ->
                    return existing.toAcceptedResponse()
                }
            }
            throw ex
        }

        if (userCredentialRepository.existsByEmail(email)) {
            commandStatusService.markRejected(
                commandId = commandId,
                code = "conflict",
                message = "Email is already registered",
                details = mapOf("email" to email)
            )
            throw ConflictException("Email is already registered", details = mapOf("email" to email))
        }

        try {
            userCredentialRepository.save(
                UserCredentialEntity(
                    userId = userId,
                    email = email,
                    passwordHash = passwordEncoder.encode(request.password),
                    status = UserStatus.ACTIVE.name,
                    createdAt = submittedAt,
                    updatedAt = submittedAt
                )
            )
        } catch (ex: DataIntegrityViolationException) {
            commandStatusService.markRejected(
                commandId = commandId,
                code = "conflict",
                message = "Email is already registered",
                details = mapOf("email" to email)
            )
            throw ConflictException("Email is already registered", details = mapOf("email" to email))
        }

        try {
            val commandMessage = GenericCommandMessage.asCommandMessage<RegisterUserCommand>(
                RegisterUserCommand(
                    userId = userId,
                    email = email,
                    displayName = displayName,
                    registeredAt = submittedAt
                )
            ).andMetaData(
                mapOf(
                    "commandId" to commandId,
                    "correlationId" to correlationId
                )
            )
            commandGateway.sendAndWait<Any>(commandMessage)
            commandStatusService.markAccepted(commandId)
        } catch (ex: Exception) {
            logger.error("Failed to dispatch register-user command. commandId={}", commandId, ex)
            commandStatusService.markFailed(
                commandId = commandId,
                code = "internal_error",
                message = "Unable to process register-user request"
            )
            throw ex
        }

        return commandStatusService.getOrThrow(commandId).toAcceptedResponse()
    }

    private fun normalizeEmail(email: String): String {
        return email.trim().lowercase(Locale.US)
    }

    private fun normalizeIdempotencyKey(idempotencyKey: String?): String? {
        val normalized = idempotencyKey?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (normalized.length !in 8..128) {
            throw BadRequestException("Idempotency-Key must be between 8 and 128 characters")
        }
        return normalized
    }

    private fun newPrefixedId(prefix: String): String {
        return "${prefix}_${UUID.randomUUID().toString().replace("-", "")}"
    }

    private fun com.platformdemo.identity.auth.infrastructure.CommandStatusDocument.toAcceptedResponse(): CommandAcceptedResponse {
        val responseStatus = if (status == CommandProcessingStatus.RECEIVED) {
            CommandProcessingStatus.RECEIVED.name
        } else {
            CommandProcessingStatus.ACCEPTED.name
        }
        return CommandAcceptedResponse(
            commandId = commandId,
            commandType = commandType,
            status = responseStatus,
            acceptedAt = submittedAt,
            correlationId = correlationId,
            idempotencyKey = idempotencyKey
        )
    }

    companion object {
        const val REGISTER_USER_COMMAND_TYPE = "register_user"
    }
}
