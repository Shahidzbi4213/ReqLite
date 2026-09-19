package com.learn.reqlite.data.repository

import com.learn.reqlite.data.local.dao.RequestDao
import com.learn.reqlite.data.local.entity.DraftEntity
import com.learn.reqlite.data.local.entity.RequestBodyEntity
import com.learn.reqlite.data.local.entity.RequestEntity
import com.learn.reqlite.data.local.entity.RequestFieldEntity
import com.learn.reqlite.domain.model.HttpMethod
import com.learn.reqlite.domain.model.Request
import com.learn.reqlite.domain.model.RequestBody
import com.learn.reqlite.domain.model.RequestField
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FakeRequestDao : RequestDao {
    private val requests = mutableMapOf<String, RequestEntity>()
    private val fields = mutableListOf<RequestFieldEntity>()
    private val bodies = mutableMapOf<String, RequestBodyEntity>()
    private val drafts = mutableMapOf<String, DraftEntity>()
    
    private val requestsFlow = MutableStateFlow<List<RequestEntity>>(emptyList())
    private val draftsFlow = MutableStateFlow<List<DraftEntity>>(emptyList())

    private fun updateDraftsFlow() {
        draftsFlow.value = drafts.values.toList()
    }

    override suspend fun insertRequest(request: RequestEntity) {
        requests[request.id] = request
        requestsFlow.value = requests.values.toList()
    }

    override suspend fun insertRequestFields(newFields: List<RequestFieldEntity>) {
        fields.addAll(newFields)
    }

    override suspend fun insertRequestBody(body: RequestBodyEntity) {
        body.requestId?.let { bodies[it] = body }
        body.draftId?.let { bodies[it] = body }
    }

    override suspend fun insertDraft(draft: DraftEntity) {
        drafts[draft.id] = draft
        updateDraftsFlow()
    }

    override suspend fun getRequestById(id: String): RequestEntity? {
        return requests[id]
    }

    override suspend fun getFieldsForRequest(requestId: String): List<RequestFieldEntity> {
        return fields.filter { it.requestId == requestId }
    }

    override suspend fun getBodyForRequest(requestId: String): RequestBodyEntity? {
        return bodies[requestId]
    }

    override suspend fun getDraftForRequest(requestId: String): DraftEntity? =
        drafts.values.find { it.requestId == requestId }

    override suspend fun getDraftById(id: String): DraftEntity? =
        drafts[id]

    override fun getAllDrafts(): Flow<List<DraftEntity>> = draftsFlow

    override suspend fun getFieldsForDraft(draftId: String): List<RequestFieldEntity> =
        fields.filter { it.draftId == draftId }

    override suspend fun getBodyForDraft(draftId: String): RequestBodyEntity? =
        bodies[draftId]

    override fun getAllRequests(): Flow<List<RequestEntity>> =
        flowOf(requests.values.toList())

    override suspend fun deleteRequest(id: String) {
        requests.remove(id)
        fields.removeAll { it.requestId == id }
        bodies.remove(id)
        drafts.remove(id)
        requestsFlow.value = requests.values.toList()
    }

    override suspend fun deleteDraft(id: String) {
        drafts.remove(id)
        updateDraftsFlow()
    }

    override suspend fun deleteFieldsForDraft(draftId: String) {
        fields.removeAll { it.draftId == draftId }
    }

    override suspend fun deleteBodyForDraft(draftId: String) {
        bodies.remove(draftId)
    }
}

class RequestRepositoryImplTest {

    @Test
    fun testInsertAndGetRequest() = runTest {
        val dao = FakeRequestDao()
        val repo = RequestRepositoryImpl(dao)

        val request = Request(
            id = "r1",
            collectionId = "c1",
            folderId = null,
            name = "Test Request",
            method = HttpMethod.GET,
            url = "https://example.com",
            headers = listOf(RequestField("f1", "Key", "Value", true)),
            queryParams = emptyList(),
            body = RequestBody.NoBody,
            createdAt = 100L,
            updatedAt = 100L
        )

        repo.insertRequest(request)

        val retrieved = repo.getRequestById("r1")
        assertEquals(request.id, retrieved?.id)
        assertEquals(1, retrieved?.headers?.size)
        assertEquals("Key", retrieved?.headers?.first()?.key)
    }

    @Test
    fun testDraftCrudOperations() = runTest {
        val dao = FakeRequestDao()
        val repo = RequestRepositoryImpl(dao)

        val draft = com.learn.reqlite.domain.model.Draft(
            id = "d1",
            requestId = "r1",
            method = HttpMethod.POST,
            url = "https://example.com/posts",
            headers = listOf(RequestField("h1", "Content-Type", "application/json", true)),
            queryParams = listOf(RequestField("q1", "debug", "true", true)),
            body = RequestBody.TextBody("{\"test\": true}", "application/json"),
            updatedAt = 200L
        )

        repo.insertDraft(draft)

        val retrieved = repo.getDraftById("d1")
        assertEquals("d1", retrieved?.id)
        assertEquals("https://example.com/posts", retrieved?.url)
        assertEquals(HttpMethod.POST, retrieved?.method)
        assertEquals(1, retrieved?.headers?.size)

        val retrievedForReq = repo.getDraftForRequest("r1")
        assertEquals("d1", retrievedForReq?.id)

        val allDrafts = repo.getAllDrafts().first()
        assertEquals(1, allDrafts.size)
        assertEquals("d1", allDrafts.first().id)

        repo.deleteDraft("d1")
        assertNull(repo.getDraftById("d1"))
    }
}
