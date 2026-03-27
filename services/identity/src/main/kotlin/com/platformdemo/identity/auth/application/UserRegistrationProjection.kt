package com.platformdemo.identity.auth.application

import com.platformdemo.identity.auth.domain.UserRegisteredEvent
import com.platformdemo.identity.auth.infrastructure.ProducedEventRefDocument
import com.platformdemo.identity.auth.infrastructure.UserReadModelDocument
import com.platformdemo.identity.auth.infrastructure.UserReadModelRepository
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.EventMessage
import org.axonframework.messaging.annotation.MetaDataValue
import org.springframework.stereotype.Component

@Component
@ProcessingGroup("identity")
class UserRegistrationProjection(
    private val userReadModelRepository: UserReadModelRepository,
    private val commandStatusService: CommandStatusService
) {

    @EventHandler
    fun on(
        event: UserRegisteredEvent,
        eventMessage: EventMessage<UserRegisteredEvent>,
        @MetaDataValue(value = "commandId", required = false) commandId: String?
    ) {
        userReadModelRepository.save(
            UserReadModelDocument(
                id = event.userId,
                email = event.email,
                displayName = event.displayName,
                status = event.status,
                emailVerified = false,
                createdAt = event.registeredAt,
                updatedAt = eventMessage.timestamp
            )
        )

        if (!commandId.isNullOrBlank()) {
            commandStatusService.markSucceeded(
                commandId = commandId,
                producedEvent = ProducedEventRefDocument(
                    eventId = eventMessage.identifier,
                    eventType = "identity.user.registered",
                    occurredAt = eventMessage.timestamp
                )
            )
        }
    }
}
