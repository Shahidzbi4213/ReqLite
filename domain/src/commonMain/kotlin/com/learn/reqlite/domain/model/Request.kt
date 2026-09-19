package com.learn.reqlite.domain.model

data class Request(
    val id: String,
    val collectionId: String,
    val folderId: String? = null,
    val name: String,
    val method: HttpMethod,
    val url: String,
    val headers: List<RequestField> = emptyList(),
    val queryParams: List<RequestField> = emptyList(),
    val body: RequestBody = RequestBody.NoBody,
    val createdAt: Long,
    val updatedAt: Long
)
