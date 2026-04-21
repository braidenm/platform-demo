package com.platformdemo.identity.endpoint

import com.mongodb.client.MongoClient
import com.platformdemo.identity.auth.endpoint.request.LoginRequest
import com.platformdemo.identity.auth.endpoint.view.TokenSessionResponse
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
import java.security.MessageDigest
import java.time.Duration
import java.util.Locale
import java.util.UUID

class LoginApiTest : BaseTest() {

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
    fun `login should authenticate and create refresh-backed session`() {
        val password = "S3curePassw0rd!"
        val registerRequest = TestDataFactory.registerUserRequest(
            email = TestDataFactory.uniqueEmail("login-success").uppercase(Locale.US),
            password = password,
            displayName = "login-success-${UUID.randomUUID()}"
        )
        val registerResult = submitRegisterUser(
            request = registerRequest,
            idempotencyKey = newIdempotencyKey("login-success-register")
        )
        val normalizedEmail = registerResult.email

        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted {
                assertThat(countDomainEventsForUser(registerResult.userId)).isEqualTo(1L)
                assertThat(countUserProjectionByEmail(normalizedEmail)).isEqualTo(1L)
            }

        val eventsBeforeLogin = countDomainEventsForUser(registerResult.userId)
        val loginResponse = submitLogin(
            TestDataFactory.loginRequest(
                email = normalizedEmail.uppercase(Locale.US),
                password = password
            )
        )

        assertThat(loginResponse.tokenType).isEqualTo("Bearer")
        assertThat(loginResponse.expiresIn).isEqualTo(900)
        assertThat(loginResponse.refreshExpiresIn).isEqualTo(2_592_000)
        assertThat(loginResponse.accessToken).startsWith("atk_")
        assertThat(loginResponse.refreshToken).startsWith("rtk_")
        assertThat(loginResponse.refreshToken).isNotEqualTo(loginResponse.accessToken)

        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted {
                val sessionRow = jdbcTemplate.queryForMap(
                    """
                    select session_id, user_id, provider, refresh_token_hash, revoked_at
                    from identity.identity_auth_sessions
                    where user_id = ?
                    """.trimIndent(),
                    registerResult.userId
                )
                val refreshTokenHash = sessionRow["refresh_token_hash"] as String
                assertThat(sessionRow["session_id"] as String).startsWith("ses_")
                assertThat(sessionRow["user_id"]).isEqualTo(registerResult.userId)
                assertThat(sessionRow["provider"]).isEqualTo("LOCAL")
                assertThat(refreshTokenHash).hasSize(64)
                assertThat(refreshTokenHash).isEqualTo(sha256Hex(loginResponse.refreshToken))
                assertThat(refreshTokenHash).isNotEqualTo(loginResponse.refreshToken)
                assertThat(sessionRow["revoked_at"]).isNull()
                assertThat(countAuthSessionsByUser(registerResult.userId)).isEqualTo(1L)

                val projectionDocument = userProjectionCollection().find(Document("_id", registerResult.userId)).first()
                assertThat(projectionDocument).isNotNull
                assertThat(projectionDocument!!.getString("email")).isEqualTo(normalizedEmail)
                assertThat(projectionDocument.getString("status")).isEqualTo("ACTIVE")

                // Login is synchronous for now; no new Axon/Kafka domain event is published in this slice.
                assertThat(countDomainEventsForUser(registerResult.userId)).isEqualTo(eventsBeforeLogin)

                webTestClient.get()
                    .uri("/v1/users/{userId}", registerResult.userId)
                    .exchange()
                    .expectStatus().isOk
                    .expectBody(UserResponse::class.java)
                    .value { user ->
                        assertThat(user.id).isEqualTo(registerResult.userId)
                        assertThat(user.email).isEqualTo(normalizedEmail)
                        assertThat(user.status).isEqualTo("ACTIVE")
                    }
            }
    }

    @Test
    fun `login should return unauthorized for invalid credentials and avoid session write`() {
        val registerRequest = TestDataFactory.registerUserRequest(
            email = TestDataFactory.uniqueEmail("login-invalid"),
            password = "S3curePassw0rd!"
        )
        val registerResult = submitRegisterUser(
            request = registerRequest,
            idempotencyKey = newIdempotencyKey("login-invalid-register")
        )

        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted {
                assertThat(countDomainEventsForUser(registerResult.userId)).isEqualTo(1L)
            }

        val result: EntityExchangeResult<ErrorEnvelope> = webTestClient.post()
            .uri("/v1/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                TestDataFactory.loginRequest(
                    email = registerResult.email,
                    password = "WrongPassw0rd!"
                )
            )
            .exchange()
            .expectStatus().isUnauthorized
            .expectBody(ErrorEnvelope::class.java)
            .returnResult()

        val errorEnvelope = result.responseBody!!
        assertThat(errorEnvelope.error.code).isEqualTo("unauthorized")
        assertThat(errorEnvelope.error.message).isEqualTo("Invalid email or password")
        assertThat(countAuthSessionsByUser(registerResult.userId)).isEqualTo(0L)
        assertThat(countDomainEventsForUser(registerResult.userId)).isEqualTo(1L)

        webTestClient.get()
            .uri("/v1/users/{userId}", registerResult.userId)
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `login should return unauthorized for inactive credential status and avoid session write`() {
        val password = "S3curePassw0rd!"
        val registerRequest = TestDataFactory.registerUserRequest(
            email = TestDataFactory.uniqueEmail("login-inactive"),
            password = password
        )
        val registerResult = submitRegisterUser(
            request = registerRequest,
            idempotencyKey = newIdempotencyKey("login-inactive-register")
        )

        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted {
                assertThat(countDomainEventsForUser(registerResult.userId)).isEqualTo(1L)
            }

        jdbcTemplate.update(
            """
            update identity.identity_user_credentials
            set status = ?, updated_at = now()
            where user_id = ?
            """.trimIndent(),
            "INACTIVE",
            registerResult.userId
        )

        val result: EntityExchangeResult<ErrorEnvelope> = webTestClient.post()
            .uri("/v1/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                TestDataFactory.loginRequest(
                    email = registerResult.email,
                    password = password
                )
            )
            .exchange()
            .expectStatus().isUnauthorized
            .expectBody(ErrorEnvelope::class.java)
            .returnResult()

        val errorEnvelope = result.responseBody!!
        assertThat(errorEnvelope.error.code).isEqualTo("unauthorized")
        assertThat(errorEnvelope.error.message).isEqualTo("Invalid email or password")
        assertThat(countAuthSessionsByUser(registerResult.userId)).isEqualTo(0L)
        assertThat(countDomainEventsForUser(registerResult.userId)).isEqualTo(1L)
    }

    @Test
    fun `login should normalize mixed-case email before credential lookup`() {
        val password = "S3curePassw0rd!"
        val registerRequest = TestDataFactory.registerUserRequest(
            email = TestDataFactory.uniqueEmail("login-normalize"),
            password = password
        )
        val registerResult = submitRegisterUser(
            request = registerRequest,
            idempotencyKey = newIdempotencyKey("login-normalize-register")
        )

        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted {
                assertThat(countDomainEventsForUser(registerResult.userId)).isEqualTo(1L)
            }

        val loginResponse = submitLogin(
            TestDataFactory.loginRequest(
                email = registerResult.email.uppercase(Locale.US),
                password = password
            )
        )

        assertThat(loginResponse.tokenType).isEqualTo("Bearer")
        assertThat(loginResponse.accessToken).startsWith("atk_")
        assertThat(loginResponse.refreshToken).startsWith("rtk_")
        assertThat(countAuthSessionsByUser(registerResult.userId)).isEqualTo(1L)
    }

    @Test
    fun `login should return unauthorized for unknown email and keep session table unchanged`() {
        val result: EntityExchangeResult<ErrorEnvelope> = webTestClient.post()
            .uri("/v1/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                TestDataFactory.loginRequest(
                    email = TestDataFactory.uniqueEmail("login-unknown"),
                    password = "S3curePassw0rd!"
                )
            )
            .exchange()
            .expectStatus().isUnauthorized
            .expectBody(ErrorEnvelope::class.java)
            .returnResult()

        val errorEnvelope = result.responseBody!!
        assertThat(errorEnvelope.error.code).isEqualTo("unauthorized")
        assertThat(errorEnvelope.error.message).isEqualTo("Invalid email or password")
        assertThat(countAuthSessions()).isEqualTo(0L)
    }

    @Test
    fun `login should return bad request for invalid payload and keep session table unchanged`() {
        val result: EntityExchangeResult<ErrorEnvelope> = webTestClient.post()
            .uri("/v1/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                TestDataFactory.loginRequest(
                    email = "not-an-email",
                    password = "short"
                )
            )
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(ErrorEnvelope::class.java)
            .returnResult()

        val errorEnvelope = result.responseBody!!
        assertThat(errorEnvelope.error.code).isEqualTo("invalid_request")
        assertThat(errorEnvelope.error.message).isEqualTo("Request validation failed")
        assertThat(errorEnvelope.error.details).containsKeys("email", "password")
        assertThat(countAuthSessions()).isEqualTo(0L)
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

    private fun submitLogin(request: LoginRequest): TokenSessionResponse {
        val result: EntityExchangeResult<TokenSessionResponse> = webTestClient.post()
            .uri("/v1/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody(TokenSessionResponse::class.java)
            .returnResult()

        return result.responseBody!!
    }

    private fun countAuthSessionsByUser(userId: String): Long {
        return jdbcTemplate.queryForObject(
            "select count(*) from identity.identity_auth_sessions where user_id = ?",
            Long::class.java,
            userId
        ) ?: 0L
    }

    private fun countAuthSessions(): Long {
        return jdbcTemplate.queryForObject(
            "select count(*) from identity.identity_auth_sessions",
            Long::class.java
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

    private fun countUserProjectionByEmail(email: String): Long {
        return userProjectionCollection().countDocuments(Document("email", email))
    }

    private fun userProjectionCollection() = mongoClient
        .getDatabase(TEST_MONGO_DATABASE)
        .getCollection(USER_PROJECTION_COLLECTION)

    private fun newIdempotencyKey(prefix: String): String {
        return "idem-$prefix-${UUID.randomUUID().toString().replace("-", "")}"
    }

    private fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    companion object {
        private const val TEST_MONGO_DATABASE = "platformdemo_test"
        private const val USER_PROJECTION_COLLECTION = "identity_users"
    }
}
