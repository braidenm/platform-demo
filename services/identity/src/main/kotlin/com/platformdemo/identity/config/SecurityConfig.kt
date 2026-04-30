package com.platformdemo.identity.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import com.platformdemo.shared.api.ApiError
import com.platformdemo.shared.api.ErrorEnvelope
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.web.SecurityFilterChain
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(AccessTokenProperties::class)
class SecurityConfig(
    private val objectMapper: ObjectMapper,
    private val accessTokenProperties: AccessTokenProperties
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/v1/register-user",
                    "/v1/login",
                    "/v1/refresh",
                    "/v1/logout",
                    "/actuator/health",
                    "/actuator/info"
                ).permitAll()
                it.requestMatchers("/v1/session").authenticated()
                it.anyRequest().permitAll()
            }
            .oauth2ResourceServer { oauth2 -> oauth2.jwt {} }
            .exceptionHandling { handling ->
                handling.authenticationEntryPoint { request, response, _ ->
                    writeSecurityError(
                        request = request,
                        status = HttpStatus.UNAUTHORIZED,
                        code = "unauthorized",
                        message = "Missing or invalid authentication",
                        response = response
                    )
                }
                handling.accessDeniedHandler { request, response, _ ->
                    writeSecurityError(
                        request = request,
                        status = HttpStatus.FORBIDDEN,
                        code = "forbidden",
                        message = "Authenticated but not authorized",
                        response = response
                    )
                }
            }
            .headers { it.frameOptions { frameOptions -> frameOptions.disable() } } // For H2 console if used
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun jwtDecoder(): JwtDecoder {
        val decoder = NimbusJwtDecoder.withSecretKey(signingKey())
            .macAlgorithm(MacAlgorithm.HS256)
            .build()

        val audienceValidator = OAuth2TokenValidator<Jwt> { jwt ->
            if (jwt.audience.contains(accessTokenProperties.audience)) {
                OAuth2TokenValidatorResult.success()
            } else {
                OAuth2TokenValidatorResult.failure(
                    OAuth2Error(
                        "invalid_token",
                        "The required audience is missing",
                        null
                    )
                )
            }
        }

        val tokenUseValidator = OAuth2TokenValidator<Jwt> { jwt ->
            if (jwt.getClaimAsString("token_use") == ACCESS_TOKEN_USE_CLAIM_VALUE) {
                OAuth2TokenValidatorResult.success()
            } else {
                OAuth2TokenValidatorResult.failure(
                    OAuth2Error(
                        "invalid_token",
                        "The token_use claim must be access",
                        null
                    )
                )
            }
        }

        val sessionIdValidator = OAuth2TokenValidator<Jwt> { jwt ->
            val sessionId = jwt.getClaimAsString("sid")
            if (!sessionId.isNullOrBlank()) {
                OAuth2TokenValidatorResult.success()
            } else {
                OAuth2TokenValidatorResult.failure(
                    OAuth2Error(
                        "invalid_token",
                        "The sid claim is required",
                        null
                    )
                )
            }
        }

        decoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(
                JwtValidators.createDefaultWithIssuer(accessTokenProperties.issuer),
                audienceValidator,
                tokenUseValidator,
                sessionIdValidator
            )
        )

        return decoder
    }

    @Bean
    fun jwtEncoder(): JwtEncoder {
        return NimbusJwtEncoder(ImmutableSecret<SecurityContext>(signingKey()))
    }

    private fun signingKey(): SecretKey {
        return SecretKeySpec(
            accessTokenProperties.signingSecret.toByteArray(StandardCharsets.UTF_8),
            "HmacSHA256"
        )
    }

    private fun writeSecurityError(
        request: HttpServletRequest,
        response: jakarta.servlet.http.HttpServletResponse,
        status: HttpStatus,
        code: String,
        message: String
    ) {
        val requestId = request.getHeader("X-Correlation-Id")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: UUID.randomUUID().toString()

        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE

        val body = ErrorEnvelope(
            error = ApiError(
                code = code,
                message = message,
                requestId = requestId
            )
        )
        response.writer.use { writer ->
            objectMapper.writeValue(writer, body)
        }
    }

    companion object {
        private const val ACCESS_TOKEN_USE_CLAIM_VALUE = "access"
    }
}
