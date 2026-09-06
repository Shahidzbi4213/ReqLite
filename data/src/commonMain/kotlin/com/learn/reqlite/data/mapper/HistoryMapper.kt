package com.learn.reqlite.data.mapper

import com.learn.reqlite.data.local.entity.HistoryEntryEntity
import com.learn.reqlite.data.local.entity.ResponseArtifactEntity
import com.learn.reqlite.domain.model.HistoryEntry
import com.learn.reqlite.domain.model.HttpMethod
import com.learn.reqlite.domain.model.ResponseArtifact

fun HistoryEntryEntity.toDomain(): HistoryEntry {
    return HistoryEntry(
        id = id,
        requestId = requestId,
        requestMethod = HttpMethod.valueOf(requestMethod),
        requestUrl = requestUrl,
        statusCode = statusCode,
        durationMs = durationMs,
        timestamp = timestamp,
        responseArtifactId = responseArtifactId,
        errorCode = errorCode,
        errorMessage = errorMessage
    )
}

fun HistoryEntry.toEntity(): HistoryEntryEntity {
    return HistoryEntryEntity(
        id = id,
        requestId = requestId,
        requestMethod = requestMethod.name,
        requestUrl = requestUrl,
        statusCode = statusCode,
        durationMs = durationMs,
        timestamp = timestamp,
        responseArtifactId = responseArtifactId,
        errorCode = errorCode,
        errorMessage = errorMessage
    )
}

fun ResponseArtifactEntity.toDomain(): ResponseArtifact {
    return ResponseArtifact(
        id = id,
        filePath = filePath,
        contentType = contentType,
        sizeBytes = sizeBytes,
        timestamp = timestamp
    )
}

fun ResponseArtifact.toEntity(): ResponseArtifactEntity {
    return ResponseArtifactEntity(
        id = id,
        filePath = filePath,
        contentType = contentType,
        sizeBytes = sizeBytes,
        timestamp = timestamp
    )
}
