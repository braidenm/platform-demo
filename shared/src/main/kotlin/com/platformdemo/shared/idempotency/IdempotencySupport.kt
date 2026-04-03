package com.platformdemo.shared.idempotency

import jakarta.validation.ValidationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class IdempotencySupport(
    private val idempotencyRecordRepository: IdempotencyRecordRepository,
    private val clock: Clock = Clock.systemUTC()
) {

    fun normalize(key: String?): String? {
        val normalized = key?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (normalized.length !in 8..128) {
            throw ValidationException("Idempotency-Key must be between 8 and 128 characters")
        }
        return normalized
    }

    fun find(scope: String, key: String): IdempotencyRecord? {
        return idempotencyRecordRepository.findByScopeAndIdempotencyKey(scope, key)
    }

    @Transactional
    fun remember(scope: String, key: String, resourceId: String): IdempotencyRecord {
        try {
            return idempotencyRecordRepository.save(
                IdempotencyRecord(
                    recordId = "idem_${UUID.randomUUID().toString().replace("-", "")}",
                    scope = scope,
                    idempotencyKey = key,
                    resourceId = resourceId,
                    createdAt = Instant.now(clock)
                )
            )
        } catch (ex: DataIntegrityViolationException) {
            return idempotencyRecordRepository.findByScopeAndIdempotencyKey(scope, key)
                ?: throw ex
        }
    }
}
