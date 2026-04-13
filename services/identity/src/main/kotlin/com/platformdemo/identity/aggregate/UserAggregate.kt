package com.platformdemo.identity.aggregate

import com.platformdemo.identity.command.RegisterUserCommand
import com.platformdemo.identity.event.UserRegisteredEvent
import com.platformdemo.identity.event.UserStatus
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.spring.stereotype.Aggregate

@Aggregate
class UserAggregate() {

    @AggregateIdentifier
    private lateinit var userId: String
    private lateinit var email: String
    private var status: UserStatus = UserStatus.ACTIVE

    @CommandHandler
    constructor(command: RegisterUserCommand) : this() {
        AggregateLifecycle.apply(
            UserRegisteredEvent(
                userId = command.userId,
                email = command.email,
                displayName = command.displayName,
                passwordHash = command.passwordHash,
                registeredAt = command.registeredAt,
                status = UserStatus.ACTIVE,
                idempotencyKey = command.idempotencyKey
            )
        )
    }

    @EventSourcingHandler
    fun on(event: UserRegisteredEvent) {
        userId = event.userId
        email = event.email
        status = event.status
    }
}
