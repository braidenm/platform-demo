package com.platformdemo.identity.auth.service.provider

import com.platformdemo.identity.auth.service.port.RefreshTokenHasher
import org.springframework.stereotype.Component
import java.security.MessageDigest

@Component
class Sha256RefreshTokenHasher : RefreshTokenHasher {
    override fun hash(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
