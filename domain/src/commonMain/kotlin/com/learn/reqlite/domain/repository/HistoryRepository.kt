package com.learn.reqlite.domain.repository

import com.learn.reqlite.domain.model.HistoryEntry
import com.learn.reqlite.domain.model.ResponseArtifact
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    suspend fun insertHistoryEntry(entry: HistoryEntry)
    suspend fun insertResponseArtifact(artifact: ResponseArtifact)
    fun getAllHistoryEntries(): Flow<List<HistoryEntry>>
    fun getHistoryForRequest(requestId: String): Flow<List<HistoryEntry>>
    suspend fun getResponseArtifactById(id: String): ResponseArtifact?
    suspend fun deleteHistoryEntry(id: String)
    suspend fun clearHistory()
}
