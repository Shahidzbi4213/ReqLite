package com.learn.reqlite.data.repository

import com.learn.reqlite.data.local.dao.HistoryDao
import com.learn.reqlite.data.local.entity.HistoryEntryEntity
import com.learn.reqlite.data.local.entity.ResponseArtifactEntity
import com.learn.reqlite.domain.model.HistoryEntry
import com.learn.reqlite.domain.model.HttpMethod
import com.learn.reqlite.domain.model.ResponseArtifact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FakeHistoryDao : HistoryDao {
    private val entries = mutableMapOf<String, HistoryEntryEntity>()
    private val artifacts = mutableMapOf<String, ResponseArtifactEntity>()
    private val entriesFlow = MutableStateFlow<List<HistoryEntryEntity>>(emptyList())

    private fun emitFlow() {
        entriesFlow.value = entries.values.sortedByDescending { it.timestamp }
    }

    override suspend fun insertHistoryEntry(entry: HistoryEntryEntity) {
        entries[entry.id] = entry
        emitFlow()
    }

    override suspend fun insertResponseArtifact(artifact: ResponseArtifactEntity) {
        artifacts[artifact.id] = artifact
    }

    override fun getAllHistoryEntries(): Flow<List<HistoryEntryEntity>> = entriesFlow

    override fun getHistoryForRequest(requestId: String): Flow<List<HistoryEntryEntity>> {
        val flow = MutableStateFlow<List<HistoryEntryEntity>>(emptyList())
        flow.value = entries.values.filter { it.requestId == requestId }.sortedByDescending { it.timestamp }
        return flow
    }

    override suspend fun getResponseArtifactById(id: String): ResponseArtifactEntity? {
        return artifacts[id]
    }

    override suspend fun deleteHistoryEntry(id: String) {
        entries.remove(id)
        emitFlow()
    }

    override suspend fun clearHistory() {
        entries.clear()
        emitFlow()
    }
}

class HistoryRepositoryImplTest {

    @Test
    fun testInsertAndGetHistory() = runTest {
        val dao = FakeHistoryDao()
        val repo = HistoryRepositoryImpl(dao)

        val entry = HistoryEntry(
            id = "h1",
            requestId = "r1",
            requestMethod = HttpMethod.POST,
            requestUrl = "https://example.com/api",
            statusCode = 200,
            durationMs = 150L,
            timestamp = 1000L,
            responseArtifactId = "a1"
        )
        repo.insertHistoryEntry(entry)

        val artifact = ResponseArtifact(
            id = "a1",
            filePath = "/path/to/artifact.json",
            contentType = "application/json",
            sizeBytes = 1024L,
            timestamp = 1000L
        )
        repo.insertResponseArtifact(artifact)

        val retrievedArtifact = repo.getResponseArtifactById("a1")
        assertEquals(artifact.filePath, retrievedArtifact?.filePath)
    }
}
