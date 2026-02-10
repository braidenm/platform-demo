package com.platformdemo.identity.domain.user

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.spring.stereotype.Aggregate

@Aggregate
class UserAggregate() {

    @AggregateIdentifier
    private lateinit var userId: String
    private lateinit var username: String
    private lateinit var email: String

    @CommandHandler
    constructor(command: RegisterUserCommand) : this() {
        AggregateLifecycle.apply(UserRegisteredEvent(
            userId = command.userId,
            username = command.username,
            email = command.email,
            passwordHash = command.passwordHash
        ))
    }

    @EventSourcingHandler
    fun on(event: UserRegisteredEvent) {
        this.userId = event.userId
        this.username = event.username
        this.email = event.email
    }
}

data class RegisterUserCommand(
    @AggregateIdentifier
    val userId: String,
    val username: String,
    val email: String,
    val passwordHash: String
)

data class UserRegisteredEvent(
    val userId: String,
    val username: String,
    val email: String,
    val passwordHash: String
)
