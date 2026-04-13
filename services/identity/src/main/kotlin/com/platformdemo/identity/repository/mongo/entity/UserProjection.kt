package com.platformdemo.identity.repository.mongo.entity

import com.platformdemo.identity.event.UserStatus
import java.time.Instant
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

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