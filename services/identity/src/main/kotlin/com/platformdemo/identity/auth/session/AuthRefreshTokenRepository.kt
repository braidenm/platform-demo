package com.platformdemo.identity.auth.session

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AuthRefreshTokenRepository : JpaRepository<AuthRefreshTokenRecord, String> {
    fun findByRefreshTokenHash(refreshTokenHash: String): AuthRefreshTokenRecord?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from AuthRefreshTokenRecord token where token.refreshTokenHash = :refreshTokenHash")
    fun findForUpdateByRefreshTokenHash(
        @Param("refreshTokenHash") refreshTokenHash: String
    ): AuthRefreshTokenRecord?

    fun findBySessionId(sessionId: String): List<AuthRefreshTokenRecord>
}
