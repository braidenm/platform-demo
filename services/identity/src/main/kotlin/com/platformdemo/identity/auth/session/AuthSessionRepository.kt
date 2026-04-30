package com.platformdemo.identity.auth.session

import org.springframework.data.jpa.repository.JpaRepository

interface AuthSessionRepository : JpaRepository<AuthSessionRecord, String> {
    fun countByUserId(userId: String): Long
}
