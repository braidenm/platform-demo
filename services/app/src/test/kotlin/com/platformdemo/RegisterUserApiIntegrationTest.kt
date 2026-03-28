package com.platformdemo

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.platformdemo.identity.auth.infrastructure.UserReadModelRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Duration
import java.time.Instant

@AutoConfigureMockMvc
class RegisterUserApiIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userReadModelRepository: UserReadModelRepository

    @Test
    fun `register-user should accept and project user read model`() {
        val email = "register-success-${System.currentTimeMillis()}@example.com"
        val result = submitRegisterUser(
            email = email,
            idempotencyKey = "idem-${System.currentTimeMillis()}-success"
        )

        val commandId = result["command_id"].asText()
        assertThat(commandId).startsWith("cmd_")
        assertThat(result["status"].asText()).isEqualTo("ACCEPTED")
        assertThat(result["command_type"].asText()).isEqualTo("register_user")

        awaitCommandStatus(commandId, "SUCCEEDED")

        val projectedUser = userReadModelRepository.findByEmail(email).orElse(null)
        assertThat(projectedUser).isNotNull
        assertThat(projectedUser.id).startsWith("usr_")
        assertThat(projectedUser.status.name).isEqualTo("ACTIVE")
    }

    @Test
    fun `register-user should be idempotent when Idempotency-Key is reused`() {
        val idempotencyKey = "idem-${System.currentTimeMillis()}-same-key"
        val email = "register-idem-${System.currentTimeMillis()}@example.com"

        val first = submitRegisterUser(email = email, idempotencyKey = idempotencyKey)
        val second = submitRegisterUser(email = email, idempotencyKey = idempotencyKey)

        assertThat(second["command_id"].asText()).isEqualTo(first["command_id"].asText())
        assertThat(second["status"].asText()).isEqualTo("ACCEPTED")
    }

    @Test
    fun `register-user should return conflict for duplicate email with a new Idempotency-Key`() {
        val email = "register-duplicate-${System.currentTimeMillis()}@example.com"
        submitRegisterUser(email = email, idempotencyKey = "idem-${System.currentTimeMillis()}-first")

        mockMvc.perform(
            post("/v1/register-user")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "idem-${System.currentTimeMillis()}-second")
                .content(registerPayload(email))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("conflict"))
            .andExpect(jsonPath("$.error.message").value("Email is already registered"))
    }

    private fun submitRegisterUser(email: String, idempotencyKey: String): JsonNode {
        val response = mockMvc.perform(
            post("/v1/register-user")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .header("X-Correlation-Id", "corr-${System.currentTimeMillis()}")
                .content(registerPayload(email))
        )
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.command_id").exists())
            .andReturn()
            .response
            .contentAsString

        return objectMapper.readTree(response)
    }

    private fun awaitCommandStatus(commandId: String, expectedStatus: String) {
        val timeout = Duration.ofSeconds(15)
        val pollInterval = Duration.ofMillis(250)
        val deadline = Instant.now().plus(timeout)

        while (Instant.now().isBefore(deadline)) {
            val response = mockMvc.perform(get("/v1/commands/$commandId"))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString

            val currentStatus = objectMapper.readTree(response)["status"]?.asText()
            if (currentStatus == expectedStatus) {
                return
            }
            Thread.sleep(pollInterval.toMillis())
        }

        fail("Command $commandId did not reach status $expectedStatus within $timeout")
    }

    private fun registerPayload(email: String): String {
        return """
            {
              "email": "$email",
              "password": "S3curePassw0rd!",
              "display_name": "Test User"
            }
        """.trimIndent()
    }
}
