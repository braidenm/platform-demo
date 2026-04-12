package com.platformdemo.identity.handler

import com.platformdemo.identity.entity.UserProjection
import com.platformdemo.identity.event.UserRegisteredEvent
import com.platformdemo.identity.repository.UserProjectionRepository
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.EventMessage
import org.springframework.stereotype.Component

@Component
@ProcessingGroup("identity")
class UserProjectionHandler(
    private val userProjectionRepository: UserProjectionRepository
) {

    @EventHandler
    fun on(
        event: UserRegisteredEvent,
        eventMessage: EventMessage<UserRegisteredEvent>
    ) {
        userProjectionRepository.save(
            UserProjection(
                id = event.userId,
                email = event.email,
                displayName = event.displayName,
                status = event.status,
                emailVerified = false,
                createdAt = event.registeredAt,
                updatedAt = eventMessage.timestamp
            )
        )
    }
}
