package com.platformdemo.identity.auth.service.provider

import com.platformdemo.identity.config.AccessTokenProperties
import com.platformdemo.identity.auth.service.port.IssuedSessionTokens
import com.platformdemo.identity.auth.service.port.RefreshTokenHasher
import com.platformdemo.identity.auth.service.port.SessionTokenIssuer
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

@Component
class RandomSessionTokenIssuer(
    private val jwtEncoder: JwtEncoder,
    private val accessTokenProperties: AccessTokenProperties,
    private val refreshTokenHasher: RefreshTokenHasher
) : SessionTokenIssuer {

    private val secureRandom = SecureRandom()
    private val base64UrlEncoder = Base64.getUrlEncoder().withoutPadding()

    override fun issue(
        userId: String,
        sessionId: String,
        provider: String,
        issuedAt: Instant
    ): IssuedSessionTokens {
        val accessTokenExpiresAt = issuedAt.plusSeconds(ACCESS_TOKEN_TTL_SECONDS)
        val accessTokenClaims = JwtClaimsSet.builder()
            .issuer(accessTokenProperties.issuer)
            .subject(userId)
            .audience(listOf(accessTokenProperties.audience))
            .issuedAt(issuedAt)
            .expiresAt(accessTokenExpiresAt)
            .id(tokenWithPrefix("jti"))
            .claim("scope", accessTokenProperties.scope)
            .claim("token_use", "access")
            .claim("sid", sessionId)
            .claim("provider", provider)
            .build()

        val accessToken = jwtEncoder.encode(
            JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                accessTokenClaims
            )
        ).tokenValue

        val refreshToken = tokenWithPrefix("rtk")
        return IssuedSessionTokens(
            accessToken = accessToken,
            accessTokenExpiresAt = accessTokenExpiresAt,
            refreshToken = refreshToken,
            refreshTokenHash = refreshTokenHasher.hash(refreshToken),
            refreshTokenExpiresAt = issuedAt.plusSeconds(REFRESH_TOKEN_TTL_SECONDS)
        )
    }

    private fun tokenWithPrefix(prefix: String): String {
        val tokenBytes = ByteArray(TOKEN_BYTE_LENGTH)
        secureRandom.nextBytes(tokenBytes)
        return "${prefix}_${base64UrlEncoder.encodeToString(tokenBytes)}"
    }

    companion object {
        private const val TOKEN_BYTE_LENGTH = 32
        private const val ACCESS_TOKEN_TTL_SECONDS = 900L
        private const val REFRESH_TOKEN_TTL_SECONDS = 2_592_000L
    }
}
