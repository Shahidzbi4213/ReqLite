package com.learn.reqlite.domain.model

data class ResponseArtifact(
    val id: String,
    val filePath: String,
    val contentType: String? = null,
    val sizeBytes: Long,
    val timestamp: Long
)
