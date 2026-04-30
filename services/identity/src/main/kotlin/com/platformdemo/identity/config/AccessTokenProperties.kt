package com.platformdemo.identity.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "identity.auth.access-token")
data class AccessTokenProperties(
    val issuer: String = "platform-demo-identity",
    val audience: String = "platform-demo-api",
    val scope: String = "identity:session:read identity:user:read",
    val signingSecret: String = "platform-demo-dev-signing-secret-change-me-32-bytes"
) {
    init {
        require(signingSecret.toByteArray(Charsets.UTF_8).size >= MIN_SIGNING_SECRET_BYTES) {
            "identity.auth.access-token.signing-secret must be at least $MIN_SIGNING_SECRET_BYTES bytes for HS256"
        }
    }

    companion object {
        private const val MIN_SIGNING_SECRET_BYTES = 32
    }
}
