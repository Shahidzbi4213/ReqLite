package com.learn.reqlite.ui.home

import com.learn.reqlite.domain.model.Draft
import com.learn.reqlite.domain.model.HistoryEntry
import com.learn.reqlite.domain.model.HttpMethod
import com.learn.reqlite.domain.model.Request
import com.learn.reqlite.domain.model.RequestBody
import com.learn.reqlite.domain.model.ResponseArtifact
import com.learn.reqlite.domain.repository.HistoryRepository
import com.learn.reqlite.domain.repository.RequestRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeHistoryRepo: FakeHistoryRepository
    private lateinit var fakeRequestRepo: FakeRequestRepository
    private lateinit var fakeCollectionRepo: FakeCollectionRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeHistoryRepo = FakeHistoryRepository()
        fakeRequestRepo = FakeRequestRepository()
        fakeCollectionRepo = FakeCollectionRepository()
        viewModel = HomeViewModel(fakeHistoryRepo, fakeRequestRepo, fakeCollectionRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun collections_observesCollectionRepository() = runTest {
        val col = com.learn.reqlite.domain.model.Collection(
            id = "c1",
            name = "Auth APIs",
            description = "Auth endpoints",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        fakeCollectionRepo.insertCollection(col)
        assertEquals(1, viewModel.collections.value.size)
        assertEquals("Auth APIs", viewModel.collections.value.first().name)
    }

    @Test
    fun createCollection_insertsCollection() = runTest {
        viewModel.createCollection(name = "Payment APIs", description = "Payments")
        assertEquals(1, viewModel.collections.value.size)
        assertEquals("Payment APIs", viewModel.collections.value.first().name)
    }

    @Test
    fun openSavedRequest_createsDraftAndCallsComplete() = runTest {
        val request = Request(
            id = "req_1",
            collectionId = "c1",
            name = "Get Profile",
            method = HttpMethod.GET,
            url = "https://api.example.com/profile",
            createdAt = 1000L,
            updatedAt = 1000L
        )

        var loadedDraftId: String? = null
        viewModel.openSavedRequest(request) { draftId ->
            loadedDraftId = draftId
        }

        assertEquals(1, viewModel.drafts.value.size)
        val draft = viewModel.drafts.value.first()
        assertEquals(loadedDraftId, draft.id)
        assertEquals("https://api.example.com/profile", draft.url)
        assertEquals(request.id, draft.requestId)
    }

    @Test
    fun recentRequests_observesHistoryRepositoryEntries() = runTest {
        val entry = HistoryEntry(
            id = "h1",
            requestId = null,
            requestMethod = HttpMethod.GET,
            requestUrl = "https://jsonplaceholder.typicode.com/posts/1",
            statusCode = 200,
            durationMs = 120,
            timestamp = 1000L
        )

        fakeHistoryRepo.insertHistoryEntry(entry)

        assertEquals(1, viewModel.recentRequests.value.size)
        assertEquals("https://jsonplaceholder.typicode.com/posts/1", viewModel.recentRequests.value.first().requestUrl)
    }

    @Test
    fun drafts_observesRequestRepositoryDrafts() = runTest {
        val draft = Draft(
            id = "d1",
            requestId = null,
            method = HttpMethod.POST,
            url = "https://jsonplaceholder.typicode.com/posts",
            updatedAt = 2000L
        )

        fakeRequestRepo.insertDraft(draft)

        assertEquals(1, viewModel.drafts.value.size)
        assertEquals("https://jsonplaceholder.typicode.com/posts", viewModel.drafts.value.first().url)
    }

    @Test
    fun clearHistory_clearsAllHistoryEntries() = runTest {
        val entry = HistoryEntry(
            id = "h1",
            requestId = null,
            requestMethod = HttpMethod.GET,
            requestUrl = "https://jsonplaceholder.typicode.com/posts/1",
            statusCode = 200,
            timestamp = 1000L
        )
        fakeHistoryRepo.insertHistoryEntry(entry)
        assertEquals(1, viewModel.recentRequests.value.size)

        viewModel.clearHistory()
        assertEquals(0, viewModel.recentRequests.value.size)
    }

    @Test
    fun deleteDraft_removesTargetDraft() = runTest {
        val draft = Draft(
            id = "d1",
            requestId = null,
            method = HttpMethod.PUT,
            url = "https://jsonplaceholder.typicode.com/posts/1",
            updatedAt = 3000L
        )
        fakeRequestRepo.insertDraft(draft)
        assertEquals(1, viewModel.drafts.value.size)

        viewModel.deleteDraft("d1")
        assertEquals(0, viewModel.drafts.value.size)
    }

    @Test
    fun importPostmanCollection_insertsCollectionAndRequests() = runTest {
        val postmanJson = """
        {
          "info": {
            "name": "Imported Test API",
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
          },
          "item": [
            {
              "name": "Test Endpoint",
              "request": {
                "method": "POST",
                "url": "https://api.test.com/v1"
              }
            }
          ]
        }
        """.trimIndent()

        var importedCount = 0
        viewModel.importPostmanCollection(postmanJson) { result ->
            importedCount = result.getOrNull()?.requestsImported ?: 0
        }

        assertEquals(1, importedCount)
        assertEquals(1, viewModel.collections.value.size)
        assertEquals("Imported Test API", viewModel.collections.value.first().name)
        assertEquals(1, viewModel.savedRequests.value.size)
        assertEquals("Test Endpoint", viewModel.savedRequests.value.first().name)
    }

    @Test
    fun parsePostmanCollection_returnsSuccessResult() = runTest {
        val postmanJson = """
        {
          "info": {
            "name": "Parsed API",
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
          },
          "item": [
            {
              "name": "Endpoint",
              "request": {
                "method": "GET",
                "url": "https://api.test.com"
              }
            }
          ]
        }
        """.trimIndent()

        val result = viewModel.parsePostmanCollection(postmanJson)
        org.junit.Assert.assertTrue(result is com.learn.reqlite.domain.parser.PostmanParseResult.Success)
        val success = result as com.learn.reqlite.domain.parser.PostmanParseResult.Success
        assertEquals("Parsed API", success.collection.name)
        assertEquals(1, success.requests.size)
    }

    class FakeHistoryRepository : HistoryRepository {
        private val entries = MutableStateFlow<List<HistoryEntry>>(emptyList())

        override suspend fun insertHistoryEntry(entry: HistoryEntry) {
            entries.value = listOf(entry) + entries.value.filter { it.id != entry.id }
        }

        override suspend fun insertResponseArtifact(artifact: ResponseArtifact) {}
        override fun getAllHistoryEntries(): Flow<List<HistoryEntry>> = entries.asStateFlow()
        override fun getHistoryForRequest(requestId: String): Flow<List<HistoryEntry>> = entries.asStateFlow()
        override suspend fun getResponseArtifactById(id: String): ResponseArtifact? = null
        override suspend fun getHistoryEntryById(id: String): HistoryEntry? = entries.value.find { it.id == id }
        override suspend fun deleteHistoryEntry(id: String) {
            entries.value = entries.value.filter { it.id != id }
        }
        override suspend fun clearHistory() {
            entries.value = emptyList()
        }
    }

    class FakeRequestRepository : RequestRepository {
        private val requests = MutableStateFlow<List<Request>>(emptyList())
        private val drafts = MutableStateFlow<List<Draft>>(emptyList())

        override suspend fun insertRequest(request: Request) {
            requests.value = requests.value.filter { it.id != request.id } + request
        }

        override suspend fun getRequestById(id: String): Request? = requests.value.find { it.id == id }
        override fun getAllRequests(): Flow<List<Request>> = requests.asStateFlow()
        override suspend fun deleteRequest(id: String) {
            requests.value = requests.value.filter { it.id != id }
        }

        override suspend fun insertDraft(draft: Draft) {
            drafts.value = listOf(draft) + drafts.value.filter { it.id != draft.id }
        }

        override suspend fun getDraftById(id: String): Draft? = drafts.value.find { it.id == id }
        override suspend fun getDraftForRequest(requestId: String): Draft? = drafts.value.find { it.requestId == requestId }
        override fun getAllDrafts(): Flow<List<Draft>> = drafts.asStateFlow()
        override suspend fun deleteDraft(id: String) {
            drafts.value = drafts.value.filter { it.id != id }
        }
    }

    class FakeCollectionRepository : com.learn.reqlite.domain.repository.CollectionRepository {
        private val collections = MutableStateFlow<List<com.learn.reqlite.domain.model.Collection>>(emptyList())

        override suspend fun insertCollection(collection: com.learn.reqlite.domain.model.Collection) {
            collections.value = collections.value.filter { it.id != collection.id } + collection
        }

        override suspend fun updateCollection(collection: com.learn.reqlite.domain.model.Collection) {
            collections.value = collections.value.filter { it.id != collection.id } + collection
        }

        override suspend fun deleteCollection(collection: com.learn.reqlite.domain.model.Collection) {
            collections.value = collections.value.filter { it.id != collection.id }
        }

        override fun getAllCollections(): Flow<List<com.learn.reqlite.domain.model.Collection>> = collections.asStateFlow()

        override suspend fun getCollectionById(id: String): com.learn.reqlite.domain.model.Collection? =
            collections.value.find { it.id == id }
    }
}
