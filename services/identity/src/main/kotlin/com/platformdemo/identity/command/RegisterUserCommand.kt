package com.platformdemo.identity.command

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.Instant

data class RegisterUserCommand(
    @TargetAggregateIdentifier
    val userId: String,
    val email: String,
    val displayName: String?,
    val passwordHash: String,
    val registeredAt: Instant,
    val idempotencyKey: String?
)
