package com.platformdemo.identity.auth.endpoint

import com.platformdemo.identity.auth.endpoint.request.LogoutRequest
import com.platformdemo.identity.auth.endpoint.request.RefreshRequest
import com.platformdemo.identity.auth.endpoint.view.SessionResponse
import com.platformdemo.identity.auth.endpoint.view.TokenSessionResponse
import com.platformdemo.identity.auth.service.LogoutService
import com.platformdemo.identity.auth.service.RefreshService
import com.platformdemo.identity.auth.service.SessionService
import com.platformdemo.identity.auth.service.model.LoginResult
import com.platformdemo.identity.auth.service.model.LogoutCommand
import com.platformdemo.identity.auth.service.model.RefreshCommand
import com.platformdemo.identity.auth.service.model.SessionResult
import com.platformdemo.identity.endpoint.view.UserResponse
import com.platformdemo.identity.handler.NotFoundException
import com.platformdemo.identity.handler.UnauthorizedException
import com.platformdemo.identity.repository.mongo.UserProjectionRepository
import com.platformdemo.identity.repository.mongo.entity.UserProjection
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1")
class SessionEndpoint(
    private val refreshService: RefreshService,
    private val logoutService: LogoutService,
    private val sessionService: SessionService,
    private val userProjectionRepository: UserProjectionRepository
) {

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequest): TokenSessionResponse {
        val result = refreshService.refresh(
            RefreshCommand(refreshToken = request.refreshToken)
        )
        return result.toResponse()
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@Valid @RequestBody request: LogoutRequest) {
        logoutService.logout(
            LogoutCommand(refreshToken = request.refreshToken)
        )
    }

    @GetMapping("/session")
    @PreAuthorize("hasAuthority('SCOPE_identity:session:read')")
    fun getSession(@AuthenticationPrincipal jwt: Jwt): SessionResponse {
        val userId = jwt.subject
        val user = userProjectionRepository.findById(userId).orElseThrow {
            NotFoundException(
                message = "User not found",
                details = mapOf("user_id" to userId)
            )
        }
        val sessionId = jwt.getClaimAsString("sid")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: throw UnauthorizedException("Missing session id claim")
        val sessionResult = sessionService.getSession(
            userId = userId,
            sessionId = sessionId,
            accessTokenExpiresAt = jwt.expiresAt
        )
        return sessionResult.toResponse(user.toApiResponse())
    }
}

private fun LoginResult.toResponse(): TokenSessionResponse {
    return TokenSessionResponse(
        accessToken = accessToken,
        tokenType = tokenType,
        expiresIn = expiresIn,
        refreshToken = refreshToken,
        refreshExpiresIn = refreshExpiresIn,
        scope = scope
    )
}

private fun UserProjection.toApiResponse(): UserResponse {
    return UserResponse(
        id = id,
        email = email,
        status = status.name,
        emailVerified = emailVerified,
        createdAt = createdAt
    )
}

private fun SessionResult.toResponse(user: UserResponse): SessionResponse {
    return SessionResponse(
        sessionId = sessionId,
        user = user,
        provider = provider,
        sessionStatus = status.name,
        accessTokenExpiresIn = accessTokenExpiresIn,
        refreshTokenExpiresIn = refreshTokenExpiresIn
    )
}
