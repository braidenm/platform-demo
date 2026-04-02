package com.platformdemo.shared.api

data class ErrorEnvelope(
    val error: ApiError
)

data class ApiError(
    val code: String,
    val message: String,
    val requestId: String,
    val details: Map<String, Any?> = emptyMap()
)
