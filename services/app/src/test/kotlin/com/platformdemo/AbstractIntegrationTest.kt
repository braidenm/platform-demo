package com.platformdemo

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun dynamicProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { "jdbc:h2:mem:platform;MODE=PostgreSQL;DB_CLOSE_DELAY=-1" }
            registry.add("spring.datasource.driver-class-name") { "org.h2.Driver" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "update" }
            registry.add("DEFAULT_SCHEMA") { "" }
            registry.add("DB_SCHEMAS") { "" }
            
            registry.add("spring.data.mongodb.repositories.enabled") { "false" }
            registry.add("spring.data.mongodb.uri") { "mongodb://localhost:27017/test" }
            
            // Disable health checks for external services to allow "UP" status
            registry.add("management.health.mongo.enabled") { "false" }
        }
    }
}
