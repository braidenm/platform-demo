package com.platformdemo.identity.auth.service.provider

import com.platformdemo.identity.auth.service.port.AuthenticatedUser
import com.platformdemo.identity.auth.service.port.CredentialAuthenticator
import com.platformdemo.identity.repository.postgres.UserCredentialRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.util.Locale

@Component
class LocalCredentialAuthenticator(
    private val userCredentialRepository: UserCredentialRepository,
    private val passwordEncoder: PasswordEncoder
) : CredentialAuthenticator {

    override fun authenticate(email: String, password: String): AuthenticatedUser? {
        val normalizedEmail = email.trim().lowercase(Locale.US)
        val storedCredential = userCredentialRepository.findByEmail(normalizedEmail) ?: return null
        if (storedCredential.status != ACTIVE_STATUS) {
            return null
        }
        if (!passwordEncoder.matches(password, storedCredential.passwordHash)) {
            return null
        }
        return AuthenticatedUser(
            userId = storedCredential.userId,
            email = storedCredential.email,
            status = storedCredential.status
        )
    }

    companion object {
        private const val ACTIVE_STATUS = "ACTIVE"
    }
}
