package com.learn.reqlite.domain.script

data class PreRequestContext(
    val url: String,
    val method: String,
    val headers: Map<String, String> = emptyMap(),
    val queryParams: Map<String, String> = emptyMap(),
    val bodyContent: String? = null,
    val environmentVariables: Map<String, String> = emptyMap()
)

data class PostRequestContext(
    val statusCode: Int,
    val statusText: String,
    val responseTimeMs: Long,
    val responseHeaders: Map<String, String> = emptyMap(),
    val responseBody: String,
    val requestUrl: String,
    val requestMethod: String,
    val environmentVariables: Map<String, String> = emptyMap()
)
