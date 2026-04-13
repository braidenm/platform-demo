package com.platformdemo.identity.testsupport

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@SpringBootApplication(scanBasePackages = ["com.platformdemo.identity", "com.platformdemo.shared"])
@EntityScan(
    basePackages = [
        "com.platformdemo.identity.auth.session",
        "com.platformdemo.identity.repository.postgres.entity",
        "com.platformdemo.shared.idempotency",
        "org.axonframework.eventsourcing.eventstore.jpa",
        "org.axonframework.eventhandling.tokenstore.jpa"
    ]
)
@EnableJpaRepositories(
    basePackages = [
        "com.platformdemo.identity.repository",
        "com.platformdemo.identity.auth.session",
        "com.platformdemo.shared.idempotency"
    ]
)
@EnableMongoRepositories(basePackages = ["com.platformdemo.identity.repository"])
class IdentityTestApplication
