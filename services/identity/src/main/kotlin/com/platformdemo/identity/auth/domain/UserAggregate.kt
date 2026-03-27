package com.platformdemo.identity.auth.domain

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.modelling.command.TargetAggregateIdentifier
import org.axonframework.spring.stereotype.Aggregate
import java.time.Instant

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
                registeredAt = command.registeredAt,
                status = UserStatus.ACTIVE
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

data class RegisterUserCommand(
    @TargetAggregateIdentifier
    val userId: String,
    val email: String,
    val displayName: String?,
    val registeredAt: Instant
)

data class UserRegisteredEvent(
    val userId: String,
    val email: String,
    val displayName: String?,
    val registeredAt: Instant,
    val status: UserStatus
)

enum class UserStatus {
    ACTIVE,
    INACTIVE,
    DEACTIVATED
}
