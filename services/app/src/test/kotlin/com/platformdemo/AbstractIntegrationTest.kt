package com.platformdemo

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
abstract class AbstractIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("platformdemo")
            .withUsername("platformdemo")
            .withPassword("platformdemo_password")
            .withInitScript("init-schemas.sql")

        @Container
        @JvmStatic
        val mongo = MongoDBContainer("mongo:7.0")

        @JvmStatic
        @DynamicPropertySource
        fun dynamicProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.driver-class-name") { postgres.driverClassName }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.jpa.hibernate.ddl-auto") { "update" }
            registry.add("DEFAULT_SCHEMA") { "identity" }
            registry.add("DB_SCHEMAS") { "identity,public" }

            registry.add("spring.data.mongodb.uri") { mongo.getReplicaSetUrl("platformdemo_test") }
            registry.add("spring.data.mongodb.database") { "platformdemo_test" }
            registry.add("MONGO_PLATFORMDEMO_DB") { "platformdemo_test" }
        }
    }
}
