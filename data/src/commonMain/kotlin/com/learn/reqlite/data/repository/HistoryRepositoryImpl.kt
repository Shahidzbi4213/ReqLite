package com.learn.reqlite.data.repository

import com.learn.reqlite.data.local.dao.HistoryDao
import com.learn.reqlite.data.mapper.toDomain
import com.learn.reqlite.data.mapper.toEntity
import com.learn.reqlite.domain.model.HistoryEntry
import com.learn.reqlite.domain.model.ResponseArtifact
import com.learn.reqlite.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepositoryImpl(
    private val historyDao: HistoryDao
) : HistoryRepository {

    override suspend fun insertHistoryEntry(entry: HistoryEntry) {
        historyDao.insertHistoryEntry(entry.toEntity())
    }

    override suspend fun insertResponseArtifact(artifact: ResponseArtifact) {
        historyDao.insertResponseArtifact(artifact.toEntity())
    }

    override fun getAllHistoryEntries(): Flow<List<HistoryEntry>> {
        return historyDao.getAllHistoryEntries().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getHistoryEntryById(id: String): HistoryEntry? {
        return historyDao.getHistoryEntryById(id)?.toDomain()
    }

    override fun getHistoryForRequest(requestId: String): Flow<List<HistoryEntry>> {
        return historyDao.getHistoryForRequest(requestId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getResponseArtifactById(id: String): ResponseArtifact? {
        return historyDao.getResponseArtifactById(id)?.toDomain()
    }

    override suspend fun deleteHistoryEntry(id: String) {
        historyDao.deleteHistoryEntry(id)
    }

    override suspend fun clearHistory() {
        historyDao.clearHistory()
    }
}
