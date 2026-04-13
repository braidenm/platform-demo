package com.platformdemo.identity.handler

import com.platformdemo.identity.event.UserRegisteredEvent
import com.platformdemo.shared.idempotency.IdempotencySupport
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.springframework.stereotype.Component

@Component
@ProcessingGroup("identity")
class RegisterUserIdempotencyHandler(
    private val idempotencySupport: IdempotencySupport
) {

    @EventHandler
    fun on(event: UserRegisteredEvent) {
        event.idempotencyKey?.let {
            idempotencySupport.remember("identity.register-user", it, event.userId)
        }
    }
}
