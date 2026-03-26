package com.platformdemo.identity.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@RestController
@RequestMapping("/identity/v1/auth")
class AuthController {

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<RegisterResponse> {
        return ResponseEntity.status(201).body(
            RegisterResponse(
                user = sampleUser(email = request.email),
                organization = sampleOrganization(createdBy = "usr_sample_01")
            )
        )
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): TokenResponse {
        val contextType = validateContextOrThrow(request.contextType, request.orgId)
        val orgContext = request.orgId ?: "platform"
        return TokenResponse(
            accessToken = "sample.access.token.for.${request.audience}.${contextType}.${orgContext}",
            expiresIn = 900,
            refreshToken = "sample.refresh.token",
            scope = "classes:read classes:write"
        )
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshRequest): TokenResponse {
        return TokenResponse(
            accessToken = "sample.access.token.refreshed",
            expiresIn = 900,
            refreshToken = "sample.refresh.token.rotated",
            scope = "classes:read classes:write"
        )
    }

    @PostMapping("/logout")
    fun logout(@RequestBody request: LogoutRequest): ResponseEntity<Void> {
        return ResponseEntity.noContent().build()
    }
}

@RestController
@RequestMapping("/identity/v1")
class IdentityController {

    @GetMapping("/me")
    fun me(): MeResponse {
        return MeResponse(
            user = sampleUser(),
            activeContext = TokenContextDto(
                aud = "platform-api",
                contextType = "org",
                orgId = "org_sample_01",
                env = "prod",
                roles = listOf("ORG_ADMIN", "AUD_ADMIN"),
                scope = "classes:read classes:write"
            ),
            organizations = listOf(
                sampleOrganization(),
                OrganizationDto(
                    id = "org_sample_02",
                    slug = "alt-org",
                    name = "Alt Org",
                    status = "ACTIVE",
                    createdBy = "usr_sample_02"
                )
            )
        )
    }

    @PostMapping("/token/exchange")
    fun tokenExchange(@RequestBody request: TokenExchangeRequest): AccessTokenResponse {
        val contextType = validateContextOrThrow(request.contextType, request.orgId)
        val orgContext = request.orgId ?: "platform"
        return AccessTokenResponse(
            accessToken = "sample.exchanged.token.${request.audience}.${contextType}.${orgContext}.${request.env}",
            expiresIn = 600
        )
    }
}

@RestController
@RequestMapping("/identity/v1/organizations")
class OrganizationController {

    @PostMapping("/{orgId}/invites")
    fun invite(
        @PathVariable orgId: String,
        @RequestBody request: InviteRequest
    ): ResponseEntity<InviteResponse> {
        return ResponseEntity.status(201).body(
            InviteResponse(
                id = "inv_sample_01",
                orgId = orgId,
                email = request.email,
                role = request.role,
                status = "PENDING"
            )
        )
    }

    @PostMapping("/{orgId}/members/{userId}/roles")
    fun assignRole(
        @PathVariable orgId: String,
        @PathVariable userId: String,
        @RequestBody request: AssignOrgRoleRequest
    ): OrgMembershipDto {
        return OrgMembershipDto(
            organizationId = orgId,
            userId = userId,
            role = request.role,
            status = "ACTIVE"
        )
    }
}

@RestController
@RequestMapping("/identity/v1/audiences")
class AudienceGrantController {

    @PostMapping("/{audience}/grants")
    fun createGrant(
        @PathVariable audience: String,
        @RequestBody request: CreateAudienceGrantRequest
    ): ResponseEntity<AudienceGrantDto> {
        return ResponseEntity.status(201).body(
            AudienceGrantDto(
                id = "grt_sample_01",
                subjectType = request.subjectType,
                subjectId = request.subjectId,
                audience = audience,
                orgId = request.orgId,
                role = request.role,
                scopes = request.scopes.ifEmpty { listOf("classes:read") },
                status = "ACTIVE"
            )
        )
    }

    @DeleteMapping("/{audience}/grants/{grantId}")
    fun deleteGrant(
        @PathVariable audience: String,
        @PathVariable grantId: String
    ): ResponseEntity<Void> {
        return ResponseEntity.noContent().build()
    }
}

@RestController
@RequestMapping("/identity/v1/admin")
class AdminController {

    @PostMapping("/impersonation-grants")
    fun createImpersonationGrant(
        @RequestBody request: CreateImpersonationGrantRequest
    ): ResponseEntity<ImpersonationGrantDto> {
        validateContextOrThrow(request.targetScope.contextType, request.targetScope.orgId)
        return ResponseEntity.status(201).body(
            ImpersonationGrantDto(
                id = "imp_sample_01",
                actorUserId = request.actorUserId,
                targetScope = request.targetScope,
                allowed = true,
                expiresAt = request.expiresAt
            )
        )
    }
}

private fun sampleUser(email: String = "user@example.com"): UserDto {
    return UserDto(
        id = "usr_sample_01",
        email = email,
        status = "ACTIVE",
        emailVerified = true,
        createdAt = Instant.parse("2026-02-25T17:20:00Z")
    )
}

private fun sampleOrganization(createdBy: String = "usr_sample_01"): OrganizationDto {
    return OrganizationDto(
        id = "org_sample_01",
        slug = "sample-org",
        name = "Sample Org",
        status = "ACTIVE",
        createdBy = createdBy
    )
}

private fun validateContextOrThrow(contextType: String, orgId: String?): String {
    val normalized = contextType.trim().lowercase()
    return when (normalized) {
        "org" -> {
            if (orgId.isNullOrBlank()) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "orgId is required when contextType=org")
            }
            normalized
        }

        "platform" -> {
            if (!orgId.isNullOrBlank()) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "orgId must be omitted when contextType=platform")
            }
            normalized
        }

        else -> throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "contextType must be one of: platform, org"
        )
    }
}
