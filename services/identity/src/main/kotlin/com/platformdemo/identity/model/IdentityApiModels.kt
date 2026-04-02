package com.platformdemo.identity.model

import java.time.Instant

data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String,
    val audience: String,
    val contextType: String,
    val orgId: String? = null,
    val env: String = "prod"
)

data class RefreshRequest(
    val refreshToken: String
)

data class LogoutRequest(
    val refreshToken: String
)

data class TokenExchangeRequest(
    val audience: String,
    val contextType: String,
    val orgId: String? = null,
    val env: String = "prod",
    val requestedRole: String? = null,
    val asUserId: String? = null,
    val reason: String? = null
)

data class InviteRequest(
    val email: String,
    val role: String
)

data class AssignOrgRoleRequest(
    val role: String
)

data class CreateAudienceGrantRequest(
    val subjectType: String,
    val subjectId: String,
    val orgId: String? = null,
    val role: String,
    val scopes: List<String> = emptyList()
)

data class CreateImpersonationGrantRequest(
    val actorUserId: String,
    val targetScope: TargetScope,
    val expiresAt: Instant
)

data class TargetScope(
    val audience: String,
    val contextType: String,
    val orgId: String? = null
)

data class UserDto(
    val id: String,
    val email: String,
    val status: String,
    val emailVerified: Boolean,
    val createdAt: Instant
)

data class OrganizationDto(
    val id: String,
    val slug: String,
    val name: String,
    val status: String,
    val createdBy: String? = null
)

data class RegisterResponse(
    val user: UserDto,
    val organization: OrganizationDto
)

data class TokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Int,
    val refreshToken: String,
    val scope: String? = null
)

data class AccessTokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Int
)

data class TokenContextDto(
    val aud: String,
    val contextType: String,
    val orgId: String? = null,
    val env: String,
    val roles: List<String>,
    val scope: String? = null
)

data class MeResponse(
    val user: UserDto,
    val activeContext: TokenContextDto,
    val organizations: List<OrganizationDto>
)

data class InviteResponse(
    val id: String,
    val orgId: String,
    val email: String,
    val role: String,
    val status: String
)

data class OrgMembershipDto(
    val organizationId: String,
    val userId: String,
    val role: String,
    val status: String
)

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

data class ImpersonationGrantDto(
    val id: String,
    val actorUserId: String,
    val targetScope: TargetScope,
    val allowed: Boolean,
    val expiresAt: Instant
)
