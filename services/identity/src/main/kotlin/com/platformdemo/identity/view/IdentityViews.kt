package com.platformdemo.identity.view

import com.platformdemo.identity.event.UserStatus
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import java.time.Instant
import java.util.Optional

@Document("identity_users")
data class UserProjection(
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

interface UserProjectionRepository : MongoRepository<UserProjection, String> {
    fun findByEmail(email: String): Optional<UserProjection>
}
