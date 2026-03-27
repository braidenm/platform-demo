package com.platformdemo.identity.auth.infrastructure

import com.platformdemo.identity.auth.domain.UserStatus
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import java.time.Instant
import java.util.Optional

@Document("identity_users")
data class UserReadModelDocument(
    @Id
    val id: String,
    @Indexed(unique = true)
    val email: String,
    val displayName: String?,
    val status: UserStatus,
    val emailVerified: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

interface UserReadModelRepository : MongoRepository<UserReadModelDocument, String> {
    fun findByEmail(email: String): Optional<UserReadModelDocument>
}

@Document("identity_command_status")
data class CommandStatusDocument(
    @Id
    val commandId: String,
    val commandType: String,
    val status: CommandProcessingStatus,
    val submittedAt: Instant,
    val completedAt: Instant? = null,
    val correlationId: String? = null,
    @Indexed(unique = true, sparse = true)
    val idempotencyKey: String? = null,
    val error: CommandErrorDocument? = null,
    val producedEvents: List<ProducedEventRefDocument> = emptyList()
)

data class CommandErrorDocument(
    val code: String,
    val message: String,
    val details: Map<String, Any?> = emptyMap()
)

data class ProducedEventRefDocument(
    val eventId: String,
    val eventType: String,
    val occurredAt: Instant
)

enum class CommandProcessingStatus {
    RECEIVED,
    VALIDATING,
    ACCEPTED,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    REJECTED;

    fun isTerminal(): Boolean = this == SUCCEEDED || this == FAILED || this == REJECTED
}

interface CommandStatusRepository : MongoRepository<CommandStatusDocument, String> {
    fun findByIdempotencyKey(idempotencyKey: String): CommandStatusDocument?
}
