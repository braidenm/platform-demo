package com.platformdemo.identity.handler

import com.platformdemo.shared.api.ApiError
import com.platformdemo.shared.api.ErrorEnvelope
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.UUID
import org.springframework.http.ResponseEntity.*

sealed class IdentityApiException(
    val status: HttpStatus,
    val code: String,
    override val message: String,
    val details: Map<String, Any?> = emptyMap()
) : RuntimeException(message)

class BadRequestException(message: String, details: Map<String, Any?> = emptyMap()) :
    IdentityApiException(HttpStatus.BAD_REQUEST, "invalid_request", message, details)

class UnauthorizedException(message: String, details: Map<String, Any?> = emptyMap()) :
    IdentityApiException(HttpStatus.UNAUTHORIZED, "unauthorized", message, details)

class ConflictException(message: String, details: Map<String, Any?> = emptyMap()) :
    IdentityApiException(HttpStatus.CONFLICT, "conflict", message, details)

class NotFoundException(message: String, details: Map<String, Any?> = emptyMap()) :
    IdentityApiException(HttpStatus.NOT_FOUND, "not_found", message, details)

@RestControllerAdvice
class IdentityApiExceptionHandler {

    private val logger = LoggerFactory.getLogger(IdentityApiExceptionHandler::class.java)

    @ExceptionHandler(IdentityApiException::class)
    fun handleIdentityApiException(
        ex: IdentityApiException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorEnvelope> {
        return buildErrorResponse(ex.status, ex.code, ex.message, request, ex.details)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorEnvelope> {
        val details = ex.bindingResult.allErrors
            .filterIsInstance<FieldError>()
            .associate { fieldError -> fieldError.field to (fieldError.defaultMessage ?: "invalid") }
        return buildErrorResponse(
            status = HttpStatus.BAD_REQUEST,
            code = "invalid_request",
            message = "Request validation failed",
            request = request,
            details = details
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorEnvelope> {
        return buildErrorResponse(
            status = HttpStatus.BAD_REQUEST,
            code = "invalid_request",
            message = "Malformed JSON request",
            request = request
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorEnvelope> {
        logger.error("Unhandled identity API exception", ex)
        return buildErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            code = "internal_error",
            message = "Unexpected error",
            request = request
        )
    }

    private fun buildErrorResponse(
        status: HttpStatus,
        code: String,
        message: String,
        request: HttpServletRequest,
        details: Map<String, Any?> = emptyMap()
    ): ResponseEntity<ErrorEnvelope> {
        val requestId = request.getHeader("X-Correlation-Id")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: UUID.randomUUID().toString()
        return status(status).body(
            ErrorEnvelope(
                error = ApiError(
                    code = code,
                    message = message,
                    requestId = requestId,
                    details = details
                )
            )
        )
    }
}
