package com.platformdemo.identity.repository.postgres

import com.platformdemo.identity.repository.postgres.entity.UserCredentialRecord
import org.springframework.data.jpa.repository.JpaRepository

interface UserCredentialRepository : JpaRepository<UserCredentialRecord, String> {
    fun existsByEmail(email: String): Boolean
    fun findByEmail(email: String): UserCredentialRecord?
}