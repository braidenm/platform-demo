package com.platformdemo.identity.repository.mongo

import com.platformdemo.identity.repository.mongo.entity.UserProjection
import java.util.Optional
import org.springframework.data.mongodb.repository.MongoRepository

interface UserProjectionRepository : MongoRepository<UserProjection, String> {
    fun findByEmail(email: String): Optional<UserProjection>
}