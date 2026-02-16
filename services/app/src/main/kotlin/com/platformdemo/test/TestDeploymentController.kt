package com.platformdemo.test

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.web.bind.annotation.*
import java.util.*

// --- PostgreSQL ---

@Entity
@Table(name = "postgres_test")
data class PostgresTestEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val message: String
)

interface PostgresTestRepository : JpaRepository<PostgresTestEntity, Long>

// --- MongoDB ---

@Document(collection = "mongo_test")
data class MongoTestDocument(
    @Id
    val id: String? = null,
    val message: String
)

interface MongoTestRepository : MongoRepository<MongoTestDocument, String>

// --- Controller ---

@RestController
@RequestMapping("/api/test-deployment")
class TestDeploymentController(
    private val postgresRepo: PostgresTestRepository,
    private val mongoRepo: MongoTestRepository
) {
private val logger = org.slf4j.LoggerFactory.getLogger(TestDeploymentController::class.java)
    @PostMapping("/postgres")
    fun createPostgres(@RequestBody message: String): PostgresTestEntity {
        logger.info("Creating Postgres entity with message: $message")
        return postgresRepo.save(PostgresTestEntity(message = message))
    }

    @GetMapping("/postgres")
    fun getAllPostgres(): List<PostgresTestEntity> {
        return postgresRepo.findAll()
    }

    @PostMapping("/mongo")
    fun createMongo(@RequestBody message: String): MongoTestDocument {
        return mongoRepo.save(MongoTestDocument(message = message))
    }

    @GetMapping("/mongo")
    fun getAllMongo(): List<MongoTestDocument> {
        return mongoRepo.findAll()
    }

    @DeleteMapping("/cleanup")
    fun cleanup() {
        postgresRepo.deleteAll()
        mongoRepo.deleteAll()
    }
}
