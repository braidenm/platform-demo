package com.platformdemo.identity.endpoint

import com.mongodb.client.MongoClient
import com.platformdemo.identity.endpoint.request.RegisterUserRequest
import com.platformdemo.identity.endpoint.view.UserRegistrationResponse
import com.platformdemo.identity.endpoint.view.UserResponse
import com.platformdemo.identity.testsupport.BaseTest
import com.platformdemo.identity.testsupport.TestDataFactory
import com.platformdemo.shared.api.ErrorEnvelope
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.bson.Document
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.reactive.server.EntityExchangeResult
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Duration
import java.util.Locale
import java.util.UUID

class RegisterUserTest : BaseTest() {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var mongoClient: MongoClient

    @BeforeEach
    fun resetState() {
        jdbcTemplate.execute("TRUNCATE TABLE identity.identity_auth_sessions CASCADE")
        jdbcTemplate.execute("TRUNCATE TABLE identity.identity_user_credentials CASCADE")
        jdbcTemplate.execute("TRUNCATE TABLE identity.shared_idempotency_records CASCADE")
        jdbcTemplate.execute("TRUNCATE TABLE identity.token_entry CASCADE")
        jdbcTemplate.execute("TRUNCATE TABLE identity.domain_event_entry CASCADE")
        jdbcTemplate.execute("TRUNCATE TABLE identity.snapshot_event_entry CASCADE")
        userProjectionCollection().deleteMany(Document())
    }

    @Test
    fun `register-user should create user and persist write and read state`() {
        val normalizedEmail = TestDataFactory.uniqueEmail("register-success")
        val request = TestDataFactory.registerUserRequest(
            email = normalizedEmail.uppercase(Locale.US),
            displayName = "display-${UUID.randomUUID()}"
        )
        val idempotencyKey = newIdempotencyKey("register-success")

        val result = submitRegisterUser(
            request = request,
            idempotencyKey = idempotencyKey
        )

        assertThat(result.userId).startsWith("usr_")
        assertThat(result.email).isEqualTo(normalizedEmail)
        assertThat(result.status).isEqualTo("ACTIVE")

        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted {
                val credentialRow = jdbcTemplate.queryForMap(
                    """
                    select user_id, email, password_hash, status
                    from identity.identity_user_credentials
                    where user_id = ?
                    """.trimIndent(),
                    result.userId
                )
                val passwordHash = credentialRow["password_hash"] as String
                assertThat(credentialRow["email"]).isEqualTo(normalizedEmail)
                assertThat(credentialRow["status"]).isEqualTo("ACTIVE")
                assertThat(passwordHash).startsWith("$2")
                assertThat(passwordHash).isNotEqualTo(request.password)

                assertThat(countIdempotencyRecords(idempotencyKey)).isEqualTo(1L)
                assertThat(countDomainEventsForUser(result.userId)).isEqualTo(1L)

                val projectionDocument = userProjectionCollection().find(Document("_id", result.userId)).first()
                assertThat(projectionDocument).isNotNull
                assertThat(projectionDocument!!.getString("email")).isEqualTo(normalizedEmail)
                assertThat(projectionDocument.getString("status")).isEqualTo("ACTIVE")
                assertThat(projectionDocument.getBoolean("emailVerified")).isFalse()

                webTestClient.get()
                    .uri("/v1/users/{userId}", result.userId)
                    .exchange()
                    .expectStatus().isOk
                    .expectBody(UserResponse::class.java)
                    .value { user ->
                        assertThat(user.id).isEqualTo(result.userId)
                        assertThat(user.email).isEqualTo(normalizedEmail)
                        assertThat(user.status).isEqualTo("ACTIVE")
                        assertThat(user.emailVerified).isFalse
                    }
            }
    }

    @Test
    fun `register-user should be idempotent when Idempotency-Key is reused`() {
        val idempotencyKey = newIdempotencyKey("register-idem")
        val request = TestDataFactory.registerUserRequest(email = TestDataFactory.uniqueEmail("register-idem"))
        val normalizedEmail = request.email.lowercase(Locale.US)

        val first = submitRegisterUser(request = request, idempotencyKey = idempotencyKey)

        val second = submitRegisterUser(request = request, idempotencyKey = idempotencyKey)

        assertThat(second.userId).isEqualTo(first.userId)
        assertThat(second.email).isEqualTo(first.email)
        assertThat(second.status).isEqualTo(first.status)

        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted {
                assertThat(countCredentialsByEmail(normalizedEmail)).isEqualTo(1L)
                assertThat(countIdempotencyRecords(idempotencyKey)).isEqualTo(1L)
                assertThat(countDomainEventsForUser(first.userId)).isEqualTo(1L)
                assertThat(countUserProjectionByEmail(normalizedEmail)).isEqualTo(1L)

                webTestClient.get()
                    .uri("/v1/users/{userId}", first.userId)
                    .exchange()
                    .expectStatus().isOk
                    .expectBody(UserResponse::class.java)
                    .value { user ->
                        assertThat(user.id).isEqualTo(first.userId)
                        assertThat(user.email).isEqualTo(normalizedEmail)
                    }
            }
    }

    @Test
    fun `register-user should return conflict for duplicate email with a new Idempotency-Key`() {
        val request = TestDataFactory.registerUserRequest(email = TestDataFactory.uniqueEmail("register-duplicate"))
        val normalizedEmail = request.email.lowercase(Locale.US)
        val firstIdempotencyKey = newIdempotencyKey("register-duplicate-first")
        val duplicateIdempotencyKey = newIdempotencyKey("register-duplicate-second")

        val first = submitRegisterUser(request = request, idempotencyKey = firstIdempotencyKey)

        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted {
                assertThat(countCredentialsByEmail(normalizedEmail)).isEqualTo(1L)
                assertThat(countIdempotencyRecords(firstIdempotencyKey)).isEqualTo(1L)
                assertThat(countDomainEventsForUser(first.userId)).isEqualTo(1L)
            }

        val result: EntityExchangeResult<ErrorEnvelope> = webTestClient.post()
            .uri("/v1/register-user")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Idempotency-Key", duplicateIdempotencyKey)
            .bodyValue(request)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ErrorEnvelope::class.java)
            .returnResult()

        val errorEnvelope = result.responseBody!!
        assertThat(errorEnvelope.error.code).isEqualTo("conflict")
        assertThat(errorEnvelope.error.message).isEqualTo("Email is already registered")
        assertThat(errorEnvelope.error.details).containsEntry("email", normalizedEmail)

        assertThat(countCredentialsByEmail(normalizedEmail)).isEqualTo(1L)
        assertThat(countUserProjectionByEmail(normalizedEmail)).isEqualTo(1L)
        assertThat(countDomainEventsForUser(first.userId)).isEqualTo(1L)
        assertThat(countIdempotencyRecords(duplicateIdempotencyKey)).isEqualTo(0L)
    }

    @Test
    fun `register-user should return bad request for invalid idempotency key`() {
        val request = TestDataFactory.registerUserRequest(email = TestDataFactory.uniqueEmail("register-invalid-idem"))

        val result: EntityExchangeResult<ErrorEnvelope> = webTestClient.post()
            .uri("/v1/register-user")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Idempotency-Key", "short")
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(ErrorEnvelope::class.java)
            .returnResult()

        val errorEnvelope = result.responseBody!!
        assertThat(errorEnvelope.error.code).isEqualTo("invalid_request")
        assertThat(errorEnvelope.error.message).isEqualTo("Idempotency-Key must be between 8 and 128 characters")

        assertThat(countCredentialsByEmail(request.email.lowercase(Locale.US))).isEqualTo(0L)
        assertThat(countDomainEventEntries()).isEqualTo(0L)
        assertThat(countUserProjectionByEmail(request.email.lowercase(Locale.US))).isEqualTo(0L)
    }

    private fun submitRegisterUser(
        request: RegisterUserRequest,
        idempotencyKey: String
    ): UserRegistrationResponse {
        val result: EntityExchangeResult<UserRegistrationResponse> = webTestClient.post()
            .uri("/v1/register-user")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Idempotency-Key", idempotencyKey)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated
            .expectBody(UserRegistrationResponse::class.java)
            .returnResult()

        return result.responseBody!!
    }

    private fun countCredentialsByEmail(email: String): Long {
        return jdbcTemplate.queryForObject(
            "select count(*) from identity.identity_user_credentials where email = ?",
            Long::class.java,
            email
        ) ?: 0L
    }

    private fun countIdempotencyRecords(idempotencyKey: String): Long {
        return jdbcTemplate.queryForObject(
            """
            select count(*)
            from identity.shared_idempotency_records
            where scope = ? and idempotency_key = ?
            """.trimIndent(),
            Long::class.java,
            "identity.register-user",
            idempotencyKey
        ) ?: 0L
    }

    private fun countDomainEventsForUser(userId: String): Long {
        return jdbcTemplate.queryForObject(
            """
            select count(*)
            from identity.domain_event_entry
            where aggregate_identifier = ?
              and payload_type like ?
            """.trimIndent(),
            Long::class.java,
            userId,
            "%UserRegisteredEvent"
        ) ?: 0L
    }

    private fun countDomainEventEntries(): Long {
        return jdbcTemplate.queryForObject(
            "select count(*) from identity.domain_event_entry",
            Long::class.java
        ) ?: 0L
    }

    private fun countUserProjectionByEmail(email: String): Long {
        return userProjectionCollection().countDocuments(Document("email", email))
    }

    private fun userProjectionCollection() = mongoClient
        .getDatabase(TEST_MONGO_DATABASE)
        .getCollection(USER_PROJECTION_COLLECTION)

    private fun newIdempotencyKey(prefix: String): String {
        return "idem-$prefix-${UUID.randomUUID().toString().replace("-", "")}"
    }

    companion object {
        private const val TEST_MONGO_DATABASE = "platformdemo_test"
        private const val USER_PROJECTION_COLLECTION = "identity_users"
    }
}
