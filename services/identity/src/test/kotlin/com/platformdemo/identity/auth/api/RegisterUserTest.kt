package com.platformdemo.identity.endpoint

import com.platformdemo.identity.model.ErrorEnvelope
import com.platformdemo.identity.model.RegisterUserRequest
import com.platformdemo.identity.model.UserRegistrationResponse
import com.platformdemo.identity.model.UserResponse
import com.platformdemo.identity.testsupport.BaseTest
import com.platformdemo.identity.testsupport.TestDataFactory
import com.platformdemo.identity.view.UserProjectionRepository
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Duration
import org.springframework.test.web.reactive.server.expectBody

class RegisterUserTest : BaseTest() {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Autowired
    lateinit var userProjectionRepository: UserProjectionRepository

    @Test
    fun `register-user should create user and return identifier`() {
        val request = TestDataFactory.registerUserRequest()

        val result = submitRegisterUser(
            request = request,
            idempotencyKey = "idem-${System.currentTimeMillis()}-success"
        )

        assertThat(result.userId).startsWith("usr_")
        assertThat(result.email).isEqualTo(request.email)
        assertThat(result.status).isEqualTo("ACTIVE")

        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted {
                val projectedUser = userProjectionRepository.findByEmail(request.email).orElse(null)
                assertThat(projectedUser).isNotNull
                assertThat(projectedUser!!.id).isEqualTo(result.userId)
                assertThat(projectedUser.status.name).isEqualTo("ACTIVE")
            }

        webTestClient.get()
            .uri("/v1/users/{userId}", result.userId)
            .exchange()
            .expectStatus().isOk
            .expectBody<UserResponse>()
            .value { user ->
                assertThat(user.id).isEqualTo(result.userId)
                assertThat(user.email).isEqualTo(request.email)
            }
    }

    @Test
    fun `register-user should be idempotent when Idempotency-Key is reused`() {
        val idempotencyKey = "idem-${System.currentTimeMillis()}-same-key"
        val request = TestDataFactory.registerUserRequest(email = TestDataFactory.uniqueEmail("register-idem"))

        val first = submitRegisterUser(request = request, idempotencyKey = idempotencyKey)
        val second = submitRegisterUser(request = request, idempotencyKey = idempotencyKey)

        assertThat(second.userId).isEqualTo(first.userId)
        assertThat(second.email).isEqualTo(first.email)
    }

    @Test
    fun `register-user should return conflict for duplicate email with a new Idempotency-Key`() {
        val request = TestDataFactory.registerUserRequest(email = TestDataFactory.uniqueEmail("register-duplicate"))
        submitRegisterUser(request = request, idempotencyKey = "idem-${System.currentTimeMillis()}-first")

        webTestClient.post()
            .uri("/v1/register-user")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Idempotency-Key", "idem-${System.currentTimeMillis()}-second")
            .bodyValue(request)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody<ErrorEnvelope>()
            .value { errorEnvelope ->
                assertThat(errorEnvelope.error.code).isEqualTo("conflict")
                assertThat(errorEnvelope.error.message).isEqualTo("Email is already registered")
            }
    }

    private fun submitRegisterUser(
        request: RegisterUserRequest,
        idempotencyKey: String
    ): UserRegistrationResponse {
        return webTestClient.post()
            .uri("/v1/register-user")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Idempotency-Key", idempotencyKey)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated
            .expectBody<UserRegistrationResponse>()
            .returnResult()
            .responseBody!!
    }
}
