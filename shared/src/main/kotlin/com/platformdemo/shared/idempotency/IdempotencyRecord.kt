package com.platformdemo.shared.idempotency

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

@Entity
@Table(
    name = "shared_idempotency_records",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_shared_idempotency_scope_key", columnNames = ["scope", "idempotency_key"])
    ]
)
class IdempotencyRecord(
    @Id
    @Column(name = "record_id", nullable = false, updatable = false, length = 64)
    val recordId: String,
    @Column(name = "scope", nullable = false, length = 120)
    val scope: String,
    @Column(name = "idempotency_key", nullable = false, length = 128)
    val idempotencyKey: String,
    @Column(name = "resource_id", nullable = false, length = 64)
    val resourceId: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant
)

interface IdempotencyRecordRepository : JpaRepository<IdempotencyRecord, String> {
    fun findByScopeAndIdempotencyKey(scope: String, idempotencyKey: String): IdempotencyRecord?
}
