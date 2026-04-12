package com.platformdemo.identity.handler

import com.platformdemo.identity.entity.StoredUserCredential
import com.platformdemo.identity.event.UserRegisteredEvent
import com.platformdemo.identity.repository.UserCredentialRepository
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.EventMessage
import org.springframework.stereotype.Component

@Component
@ProcessingGroup("identity")
class UserCredentialHandler(
    private val userCredentialRepository: UserCredentialRepository
) {

    @EventHandler
    fun on(
        event: UserRegisteredEvent,
        eventMessage: EventMessage<UserRegisteredEvent>
    ) {
        val passwordHash = event.passwordHash
            ?: throw IllegalStateException("UserRegisteredEvent missing password hash for user ${event.userId}")

        userCredentialRepository.save(
            StoredUserCredential(
                userId = event.userId,
                email = event.email,
                passwordHash = passwordHash,
                status = event.status.name,
                createdAt = event.registeredAt,
                updatedAt = eventMessage.timestamp
            )
        )
    }
}
