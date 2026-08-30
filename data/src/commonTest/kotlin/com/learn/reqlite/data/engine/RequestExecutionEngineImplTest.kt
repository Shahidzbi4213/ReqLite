package com.learn.reqlite.data.engine

import com.learn.reqlite.domain.model.*
import com.learn.reqlite.domain.repository.*
import com.learn.reqlite.domain.usecase.RequestValidator
import com.learn.reqlite.domain.usecase.VariableResolver
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeRequestRepository : RequestRepository {
    val drafts = mutableMapOf<String, Draft>()
    override suspend fun insertRequest(request: Request) {}
    override suspend fun getRequestById(id: String): Request? = null
    override fun getAllRequests(): Flow<List<Request>> = flowOf()
    override suspend fun deleteRequest(id: String) {}
    override suspend fun insertDraft(draft: Draft) { drafts[draft.id] = draft }
    override suspend fun getDraftById(id: String): Draft? = drafts[id]
    override suspend fun getDraftForRequest(requestId: String): Draft? = drafts.values.find { it.requestId == requestId }
    override fun getAllDrafts(): Flow<List<Draft>> = flowOf(drafts.values.toList())
    override suspend fun deleteDraft(id: String) { drafts.remove(id) }
}

class FakeEnvironmentRepository : EnvironmentRepository {
    val envs = mutableMapOf<String, Environment>()
    override suspend fun insertEnvironment(environment: Environment) {}
    override suspend fun updateEnvironment(environment: Environment) {}
    override suspend fun deleteEnvironment(id: String) {}
    override fun getAllEnvironments(): Flow<List<Environment>> = flowOf()
    override suspend fun getEnvironmentById(id: String): Environment? = envs[id]
}

class FakeSecureStorage : SecureStorage {
    val secrets = mutableMapOf<String, String>()
    override suspend fun saveSecret(key: String, value: String) { secrets[key] = value }
    override suspend fun getSecret(key: String): String? = secrets[key]
    override suspend fun deleteSecret(key: String) { secrets.remove(key) }
}

class FakeHistoryRepository : HistoryRepository {
    val entries = mutableListOf<HistoryEntry>()
    val artifacts = mutableListOf<ResponseArtifact>()
    
    override suspend fun insertHistoryEntry(entry: HistoryEntry) {
        entries.removeAll { it.id == entry.id }
        entries.add(entry)
    }
    override suspend fun insertResponseArtifact(artifact: ResponseArtifact) { artifacts.add(artifact) }
    override fun getAllHistoryEntries(): Flow<List<HistoryEntry>> = flowOf(entries)
    override fun getHistoryForRequest(requestId: String): Flow<List<HistoryEntry>> = flowOf(entries.filter { it.requestId == requestId })
    override suspend fun getResponseArtifactById(id: String): ResponseArtifact? = artifacts.find { it.id == id }
    override suspend fun deleteHistoryEntry(id: String) {}
    override suspend fun clearHistory() {}
}

class RequestExecutionEngineImplTest {
    
    @Test
    fun testExecutePipeline() = runTest {
        val requestRepo = FakeRequestRepository()
        val envRepo = FakeEnvironmentRepository()
        val secureStorage = FakeSecureStorage()
        val historyRepo = FakeHistoryRepository()
        val variableResolver = VariableResolver()
        val requestValidator = RequestValidator(variableResolver)
        
        requestRepo.drafts["d1"] = Draft(
            id = "d1",
            requestId = "r1",
            method = HttpMethod.GET,
            url = "https://example.com/{{path}}",
            updatedAt = 0L
        )
        
        envRepo.envs["e1"] = Environment(
            id = "e1",
            name = "Test",
            variables = listOf(
                Variable("v1", "path", "api/test", isEnabled = true)
            ),
            createdAt = 0L,
            updatedAt = 0L
        )
        
        val mockEngine = MockEngine { request ->
            assertEquals("https://example.com/api/test", request.url.toString())
            respond(
                content = "success",
                status = HttpStatusCode.OK
            )
        }
        val httpClient = HttpClient(mockEngine)
        
        val engine = RequestExecutionEngineImpl(
            httpClient,
            requestRepo,
            envRepo,
            secureStorage,
            variableResolver,
            requestValidator,
            historyRepo
        )
        
        val result = engine.execute("d1", "e1")
        
        assertEquals(200, result.statusCode)
        assertEquals("https://example.com/api/test", result.requestUrl)
        
        val artifactId = result.responseArtifactId
        assertTrue(artifactId != null)
        
        val artifact = historyRepo.artifacts.find { it.id == artifactId }
        assertTrue(artifact != null)
        assertEquals("memory", artifact.filePath)
    }
}
