package com.learn.reqlite.domain.model

data class HistoryEntry(
    val id: String,
    val requestId: String? = null,
    val requestMethod: HttpMethod,
    val requestUrl: String,
    val statusCode: Int? = null,
    val durationMs: Long? = null,
    val timestamp: Long,
    val responseArtifactId: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null
)
