package com.learn.reqlite.domain.model

data class Draft(
    val id: String,
    val requestId: String,
    val method: HttpMethod,
    val url: String,
    val headers: List<RequestField> = emptyList(),
    val queryParams: List<RequestField> = emptyList(),
    val body: RequestBody = RequestBody.NoBody,
    val updatedAt: Long
)
