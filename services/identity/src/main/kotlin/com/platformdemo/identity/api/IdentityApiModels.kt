package com.platformdemo.identity.api

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.time.Instant

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String? = null
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class LoginRequest(
    val email: String,
    val password: String,
    val audience: String,
    val contextType: String,
    val orgId: String? = null,
    val env: String = "prod"
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class RefreshRequest(
    val refreshToken: String
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class LogoutRequest(
    val refreshToken: String
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class TokenExchangeRequest(
    val audience: String,
    val contextType: String,
    val orgId: String? = null,
    val env: String = "prod",
    val requestedRole: String? = null,
    val asUserId: String? = null,
    val reason: String? = null
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class InviteRequest(
    val email: String,
    val role: String
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class AssignOrgRoleRequest(
    val role: String
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class CreateAudienceGrantRequest(
    val subjectType: String,
    val subjectId: String,
    val orgId: String? = null,
    val role: String,
    val scopes: List<String> = emptyList()
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class CreateImpersonationGrantRequest(
    val actorUserId: String,
    val targetScope: TargetScope,
    val expiresAt: Instant
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class TargetScope(
    val audience: String,
    val contextType: String,
    val orgId: String? = null
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class UserDto(
    val id: String,
    val email: String,
    val status: String,
    val emailVerified: Boolean,
    val createdAt: Instant
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class OrganizationDto(
    val id: String,
    val slug: String,
    val name: String,
    val status: String,
    val createdBy: String? = null
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class RegisterResponse(
    val user: UserDto,
    val organization: OrganizationDto
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class TokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Int,
    val refreshToken: String,
    val scope: String? = null
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class AccessTokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Int
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class TokenContextDto(
    val aud: String,
    val contextType: String,
    val orgId: String? = null,
    val env: String,
    val roles: List<String>,
    val scope: String? = null
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class MeResponse(
    val user: UserDto,
    val activeContext: TokenContextDto,
    val organizations: List<OrganizationDto>
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class InviteResponse(
    val id: String,
    val orgId: String,
    val email: String,
    val role: String,
    val status: String
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class OrgMembershipDto(
    val organizationId: String,
    val userId: String,
    val role: String,
    val status: String
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class AudienceGrantDto(
    val id: String,
    val subjectType: String,
    val subjectId: String,
    val audience: String,
    val orgId: String? = null,
    val role: String,
    val scopes: List<String>,
    val status: String
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class ImpersonationGrantDto(
    val id: String,
    val actorUserId: String,
    val targetScope: TargetScope,
    val allowed: Boolean,
    val expiresAt: Instant
)
