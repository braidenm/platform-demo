package com.platformdemo.identity.auth.application

import com.platformdemo.identity.auth.infrastructure.CommandErrorDocument
import com.platformdemo.identity.auth.infrastructure.CommandProcessingStatus
import com.platformdemo.identity.auth.infrastructure.CommandStatusDocument
import com.platformdemo.identity.auth.infrastructure.CommandStatusRepository
import com.platformdemo.identity.auth.infrastructure.ProducedEventRefDocument
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

@Service
class CommandStatusService(
    private val commandStatusRepository: CommandStatusRepository,
    private val clock: Clock = Clock.systemUTC()
) {

    fun findByIdempotencyKey(idempotencyKey: String): CommandStatusDocument? {
        return commandStatusRepository.findByIdempotencyKey(idempotencyKey)
    }

    fun createReceived(
        commandId: String,
        commandType: String,
        submittedAt: Instant,
        correlationId: String?,
        idempotencyKey: String?
    ): CommandStatusDocument {
        return commandStatusRepository.save(
            CommandStatusDocument(
                commandId = commandId,
                commandType = commandType,
                status = CommandProcessingStatus.RECEIVED,
                submittedAt = submittedAt,
                correlationId = correlationId,
                idempotencyKey = idempotencyKey
            )
        )
    }

    fun markAccepted(commandId: String) {
        val current = find(commandId) ?: return
        if (current.status.isTerminal() || current.status == CommandProcessingStatus.ACCEPTED) {
            return
        }
        commandStatusRepository.save(
            current.copy(status = CommandProcessingStatus.ACCEPTED)
        )
    }

    fun markRejected(commandId: String, code: String, message: String, details: Map<String, Any?> = emptyMap()) {
        val current = find(commandId) ?: return
        if (current.status.isTerminal()) {
            return
        }
        commandStatusRepository.save(
            current.copy(
                status = CommandProcessingStatus.REJECTED,
                completedAt = Instant.now(clock),
                error = CommandErrorDocument(code = code, message = message, details = details)
            )
        )
    }

    fun markFailed(commandId: String, code: String, message: String, details: Map<String, Any?> = emptyMap()) {
        val current = find(commandId) ?: return
        if (current.status.isTerminal()) {
            return
        }
        commandStatusRepository.save(
            current.copy(
                status = CommandProcessingStatus.FAILED,
                completedAt = Instant.now(clock),
                error = CommandErrorDocument(code = code, message = message, details = details)
            )
        )
    }

    fun markSucceeded(commandId: String, producedEvent: ProducedEventRefDocument) {
        val current = find(commandId) ?: return
        if (current.status == CommandProcessingStatus.REJECTED || current.status == CommandProcessingStatus.FAILED) {
            return
        }

        val existingEvents = current.producedEvents.associateBy { it.eventId }.toMutableMap()
        existingEvents[producedEvent.eventId] = producedEvent

        commandStatusRepository.save(
            current.copy(
                status = CommandProcessingStatus.SUCCEEDED,
                completedAt = current.completedAt ?: Instant.now(clock),
                producedEvents = existingEvents.values.toList()
            )
        )
    }

    fun getOrThrow(commandId: String): CommandStatusDocument {
        return find(commandId) ?: throw com.platformdemo.identity.auth.api.NotFoundException(
            message = "Command not found",
            details = mapOf("command_id" to commandId)
        )
    }

    private fun find(commandId: String): CommandStatusDocument? {
        return commandStatusRepository.findById(commandId).orElse(null)
    }
}
