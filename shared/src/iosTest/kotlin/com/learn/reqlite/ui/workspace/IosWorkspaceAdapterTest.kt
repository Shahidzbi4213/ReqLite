package com.learn.reqlite.ui.workspace

import com.learn.reqlite.domain.engine.RequestExecutionEngine
import com.learn.reqlite.domain.model.*
import com.learn.reqlite.domain.repository.EnvironmentRepository
import com.learn.reqlite.domain.repository.HistoryRepository
import com.learn.reqlite.domain.repository.RequestRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class IosWorkspaceAdapterTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeEngine: FakeEngine
    private lateinit var fakeEnvRepo: FakeEnvRepo
    private lateinit var fakeReqRepo: FakeReqRepo
    private lateinit var fakeHistoryRepo: FakeHistoryRepo
    private lateinit var adapter: IosWorkspaceAdapter

    @BeforeTest
    fun setup() {
        fakeEngine = FakeEngine()
        fakeEnvRepo = FakeEnvRepo()
        fakeReqRepo = FakeReqRepo()
        fakeHistoryRepo = FakeHistoryRepo()
        adapter = IosWorkspaceAdapter(
            executionEngine = fakeEngine,
            environmentRepository = fakeEnvRepo,
            requestRepository = fakeReqRepo,
            historyRepository = fakeHistoryRepo,
            scope = CoroutineScope(testDispatcher)
        )
    }

    @AfterTest
    fun tearDown() {
        adapter.close()
    }

    @Test
    fun adapter_createsQuickDraft_and_executes_successfully() = runTest {
        var createdDraftId: String? = null
        adapter.createQuickDraft(methodName = "POST", url = "https://api.example.com/items") { id ->
            createdDraftId = id
        }

        assertNotNull(createdDraftId)
        val savedDraft = fakeReqRepo.getDraftById(createdDraftId)
        assertNotNull(savedDraft)
        assertEquals(HttpMethod.POST, savedDraft.method)
        assertEquals("https://api.example.com/items", savedDraft.url)

        val states = mutableListOf<IosExecutionState>()
        adapter.executeRequest(
            draftId = createdDraftId,
            environmentId = "env_ios_prod"
        ) { state ->
            states.add(state)
        }

        assertTrue(states.any { it is IosExecutionState.Success })
        assertEquals("env_ios_prod", fakeEngine.lastEnvironmentId)
    }

    @Test
    fun adapter_observesHistory() = runTest {
        val observed = mutableListOf<List<HistoryEntry>>()
        adapter.observeHistory { list ->
            observed.add(list)
        }

        assertTrue(observed.isNotEmpty())
        assertEquals("https://api.example.com/history-item", observed.first().first().requestUrl)
    }

    @Test
    fun adapter_observesEnvironments() = runTest {
        val observed = mutableListOf<List<Environment>>()
        adapter.observeEnvironments { envs ->
            observed.add(envs)
        }

        assertTrue(observed.isNotEmpty())
        assertEquals("Development", observed.first().first().name)
    }

    @Test
    fun adapter_handlesCancellation() {
        var finalState: IosExecutionState? = null
        adapter.cancelExecution { state ->
            finalState = state
        }

        assertEquals(IosExecutionState.Idle, finalState)
    }

    class FakeEngine : RequestExecutionEngine {
        var lastEnvironmentId: String? = null

        override suspend fun execute(draftId: String, environmentId: String?): Pair<HistoryEntry, String> {
            lastEnvironmentId = environmentId
            val entry = HistoryEntry(
                id = "hist_1",
                requestId = draftId,
                requestMethod = HttpMethod.POST,
                requestUrl = "https://api.example.com/items",
                statusCode = 200,
                durationMs = 45,
                timestamp = 1000L
            )
            return Pair(entry, "response_artifact_id")
        }
    }

    class FakeEnvRepo : EnvironmentRepository {
        private val envs = listOf(
            Environment(id = "env_1", name = "Development", createdAt = 100L, updatedAt = 100L)
        )

        override fun getAllEnvironments(): Flow<List<Environment>> = flowOf(envs)
        override suspend fun getEnvironmentById(id: String): Environment? = envs.find { it.id == id }
        override suspend fun insertEnvironment(environment: Environment) {}
        override suspend fun updateEnvironment(environment: Environment) {}
        override suspend fun deleteEnvironment(id: String) {}
    }

    class FakeReqRepo : RequestRepository {
        private val requests = mutableMapOf<String, Request>()
        private val drafts = mutableMapOf<String, Draft>()

        override suspend fun insertRequest(request: Request) {
            requests[request.id] = request
        }

        override suspend fun getRequestById(id: String): Request? = requests[id]
        override fun getAllRequests(): Flow<List<Request>> = flowOf(requests.values.toList())
        override suspend fun deleteRequest(id: String) { requests.remove(id) }
        override suspend fun insertDraft(draft: Draft) { drafts[draft.id] = draft }
        override suspend fun getDraftById(id: String): Draft? = drafts[id]
        override suspend fun getDraftForRequest(requestId: String): Draft? = null
        override fun getAllDrafts(): Flow<List<Draft>> = flowOf(drafts.values.toList())
        override suspend fun deleteDraft(id: String) { drafts.remove(id) }
    }

    class FakeHistoryRepo : HistoryRepository {
        private val historyEntries = mutableListOf(
            HistoryEntry(
                id = "hist_sample_1",
                requestId = "draft_1",
                requestMethod = HttpMethod.GET,
                requestUrl = "https://api.example.com/history-item",
                statusCode = 200,
                durationMs = 120L,
                timestamp = 1000L
            )
        )

        override suspend fun insertHistoryEntry(entry: HistoryEntry) {
            historyEntries.add(entry)
        }

        override suspend fun insertResponseArtifact(artifact: ResponseArtifact) {}

        override fun getAllHistoryEntries(): Flow<List<HistoryEntry>> = flowOf(historyEntries)

        override fun getHistoryForRequest(requestId: String): Flow<List<HistoryEntry>> =
            flowOf(historyEntries.filter { it.requestId == requestId })

        override suspend fun getHistoryEntryById(id: String): HistoryEntry? =
            historyEntries.find { it.id == id }

        override suspend fun getResponseArtifactById(id: String): ResponseArtifact? = null

        override suspend fun deleteHistoryEntry(id: String) {
            historyEntries.removeAll { it.id == id }
        }

        override suspend fun clearHistory() {
            historyEntries.clear()
        }
    }
}
