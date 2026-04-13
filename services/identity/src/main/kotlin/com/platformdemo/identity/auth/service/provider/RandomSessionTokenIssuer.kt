package com.platformdemo.identity.auth.service.provider

import com.platformdemo.identity.auth.service.port.IssuedSessionTokens
import com.platformdemo.identity.auth.service.port.SessionTokenIssuer
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

@Component
class RandomSessionTokenIssuer : SessionTokenIssuer {

    private val secureRandom = SecureRandom()
    private val base64UrlEncoder = Base64.getUrlEncoder().withoutPadding()

    override fun issue(userId: String, issuedAt: Instant): IssuedSessionTokens {
        val accessToken = tokenWithPrefix("atk")
        val refreshToken = tokenWithPrefix("rtk")
        return IssuedSessionTokens(
            accessToken = accessToken,
            accessTokenExpiresAt = issuedAt.plusSeconds(ACCESS_TOKEN_TTL_SECONDS),
            refreshToken = refreshToken,
            refreshTokenHash = sha256Hex(refreshToken),
            refreshTokenExpiresAt = issuedAt.plusSeconds(REFRESH_TOKEN_TTL_SECONDS)
        )
    }

    private fun tokenWithPrefix(prefix: String): String {
        val tokenBytes = ByteArray(TOKEN_BYTE_LENGTH)
        secureRandom.nextBytes(tokenBytes)
        return "${prefix}_${base64UrlEncoder.encodeToString(tokenBytes)}"
    }

    private fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    companion object {
        private const val TOKEN_BYTE_LENGTH = 32
        private const val ACCESS_TOKEN_TTL_SECONDS = 900L
        private const val REFRESH_TOKEN_TTL_SECONDS = 2_592_000L
    }
}
