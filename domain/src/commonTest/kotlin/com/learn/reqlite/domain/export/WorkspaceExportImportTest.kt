package com.learn.reqlite.domain.export

import com.learn.reqlite.domain.model.*
import com.learn.reqlite.domain.repository.CollectionRepository
import com.learn.reqlite.domain.repository.EnvironmentRepository
import com.learn.reqlite.domain.repository.RequestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class WorkspaceExportImportTest {

    private lateinit var fakeCollectionRepo: FakeCollectionRepository
    private lateinit var fakeRequestRepo: FakeRequestRepository
    private lateinit var fakeEnvironmentRepo: FakeEnvironmentRepository
    private lateinit var manager: WorkspaceExportImportManager

    @BeforeTest
    fun setup() {
        fakeCollectionRepo = FakeCollectionRepository()
        fakeRequestRepo = FakeRequestRepository()
        fakeEnvironmentRepo = FakeEnvironmentRepository()
        manager = WorkspaceExportImportManager(
            collectionRepository = fakeCollectionRepo,
            requestRepository = fakeRequestRepo,
            environmentRepository = fakeEnvironmentRepo
        )
    }

    @Test
    fun exportWorkspace_exportsAllCollectionsAndEnvironments_withSecretMasking() = runTest {
        val col1 = Collection(id = "c1", name = "Auth API", description = "Endpoints for auth", createdAt = 100L, updatedAt = 100L)
        fakeCollectionRepo.insertCollection(col1)

        val req1 = Request(
            id = "r1",
            collectionId = "c1",
            name = "Login",
            method = HttpMethod.POST,
            url = "https://api.example.com/login",
            headers = listOf(RequestField(id = "h1", key = "Content-Type", value = "application/json")),
            queryParams = listOf(RequestField(id = "q1", key = "client", value = "mobile")),
            body = RequestBody.TextBody(content = "{\"user\":\"test\"}", contentType = "application/json"),
            createdAt = 100L,
            updatedAt = 100L
        )
        fakeRequestRepo.insertRequest(req1)

        val env1 = Environment(
            id = "e1",
            name = "Production",
            color = "#FF0000",
            variables = listOf(
                Variable(id = "v1", key = "baseUrl", value = "https://api.example.com", isSecret = false),
                Variable(id = "v2", key = "apiKey", value = "super_secret_123", isSecret = true)
            ),
            createdAt = 100L,
            updatedAt = 100L
        )
        fakeEnvironmentRepo.insertEnvironment(env1)

        val json = manager.exportWorkspace(includeSecrets = false)
        assertTrue(json.contains("\"schemaVersion\": 1"))
        assertTrue(json.contains("\"appName\": \"ReqLite\""))
        assertTrue(json.contains("Auth API"))
        assertTrue(json.contains("https://api.example.com/login"))
        assertTrue(json.contains("\"key\": \"baseUrl\""))
        assertTrue(json.contains("\"value\": \"https://api.example.com\""))
        assertTrue(json.contains("\"key\": \"apiKey\""))
        assertTrue(json.contains("\"value\": \"\"")) // Secret masked!
        assertFalse(json.contains("super_secret_123"))
    }

    @Test
    fun exportWorkspace_includesSecrets_when_flag_is_true() = runTest {
        val env1 = Environment(
            id = "e1",
            name = "Staging",
            variables = listOf(
                Variable(id = "v1", key = "token", value = "secret_jwt_token", isSecret = true)
            ),
            createdAt = 100L,
            updatedAt = 100L
        )
        fakeEnvironmentRepo.insertEnvironment(env1)

        val json = manager.exportWorkspace(includeSecrets = true)
        assertTrue(json.contains("secret_jwt_token"))
    }

    @Test
    fun exportCollection_exportsOnlyTargetCollection() = runTest {
        val col1 = Collection(id = "c1", name = "Collection 1", createdAt = 100L, updatedAt = 100L)
        val col2 = Collection(id = "c2", name = "Collection 2", createdAt = 100L, updatedAt = 100L)
        fakeCollectionRepo.insertCollection(col1)
        fakeCollectionRepo.insertCollection(col2)

        val json = manager.exportCollection("c1")
        assertTrue(json.contains("Collection 1"))
        assertFalse(json.contains("Collection 2"))
    }

    @Test
    fun exportEnvironment_exportsOnlyTargetEnvironment() = runTest {
        val env1 = Environment(id = "e1", name = "Env 1", createdAt = 100L, updatedAt = 100L)
        val env2 = Environment(id = "e2", name = "Env 2", createdAt = 100L, updatedAt = 100L)
        fakeEnvironmentRepo.insertEnvironment(env1)
        fakeEnvironmentRepo.insertEnvironment(env2)

        val json = manager.exportEnvironment("e2")
        assertFalse(json.contains("Env 1"))
        assertTrue(json.contains("Env 2"))
    }

    @Test
    fun importWorkspace_createsNewEntities_with_CREATE_NEW_strategy() = runTest {
        val jsonPayload = """
        {
          "schemaVersion": 1,
          "appName": "ReqLite",
          "exportedAt": 1000,
          "collections": [
            {
              "id": "c_imported",
              "name": "Payments API",
              "requests": [
                {
                  "id": "r_imported",
                  "collectionId": "c_imported",
                  "name": "Charge",
                  "method": "POST",
                  "url": "https://api.stripe.com/v1/charges",
                  "body": {
                    "type": "URL_ENCODED",
                    "fields": [
                      { "id": "f1", "key": "amount", "value": "2000" },
                      { "id": "f2", "key": "currency", "value": "usd" }
                    ]
                  }
                }
              ]
            }
          ],
          "environments": [
            {
              "id": "e_imported",
              "name": "Stripe Live",
              "variables": [
                { "id": "v1", "key": "stripeKey", "value": "sk_live_123", "isSecret": true }
              ]
            }
          ]
        }
        """.trimIndent()

        val result = manager.importWorkspace(jsonPayload, ImportStrategy.CREATE_NEW)
        assertIs<WorkspaceImportResult.Success>(result)
        assertEquals(1, result.collectionsImported)
        assertEquals(1, result.requestsImported)
        assertEquals(1, result.environmentsImported)

        val savedCol = fakeCollectionRepo.getCollectionById("c_imported")
        assertNotNull(savedCol)
        assertEquals("Payments API", savedCol.name)

        val savedReq = fakeRequestRepo.getRequestById("r_imported")
        assertNotNull(savedReq)
        assertEquals(HttpMethod.POST, savedReq.method)
        assertIs<RequestBody.UrlEncodedBody>(savedReq.body)
        assertEquals(2, savedReq.body.fields.size)

        val savedEnv = fakeEnvironmentRepo.getEnvironmentById("e_imported")
        assertNotNull(savedEnv)
        assertEquals("Stripe Live", savedEnv.name)
        assertTrue(savedEnv.variables.first().isSecret)
    }

    @Test
    fun importWorkspace_overwrites_existing_with_OVERWRITE_EXISTING_strategy() = runTest {
        val col = Collection(id = "c1", name = "Old Name", createdAt = 100L, updatedAt = 100L)
        fakeCollectionRepo.insertCollection(col)

        val jsonPayload = """
        {
          "schemaVersion": 1,
          "appName": "ReqLite",
          "exportedAt": 1000,
          "collections": [
            {
              "id": "c1",
              "name": "Updated Name"
            }
          ]
        }
        """.trimIndent()

        val result = manager.importWorkspace(jsonPayload, ImportStrategy.OVERWRITE_EXISTING)
        assertIs<WorkspaceImportResult.Success>(result)

        val updated = fakeCollectionRepo.getCollectionById("c1")
        assertNotNull(updated)
        assertEquals("Updated Name", updated.name)
    }

    @Test
    fun importWorkspace_skips_existing_with_SKIP_EXISTING_strategy() = runTest {
        val col = Collection(id = "c1", name = "Original Name", createdAt = 100L, updatedAt = 100L)
        fakeCollectionRepo.insertCollection(col)

        val jsonPayload = """
        {
          "schemaVersion": 1,
          "appName": "ReqLite",
          "exportedAt": 1000,
          "collections": [
            {
              "id": "c1",
              "name": "Overwritten Name"
            }
          ]
        }
        """.trimIndent()

        val result = manager.importWorkspace(jsonPayload, ImportStrategy.SKIP_EXISTING)
        assertIs<WorkspaceImportResult.Success>(result)
        assertEquals(0, result.collectionsImported)

        val colAfter = fakeCollectionRepo.getCollectionById("c1")
        assertEquals("Original Name", colAfter?.name)
    }

    @Test
    fun importWorkspace_handles_all_body_types() = runTest {
        val jsonPayload = """
        {
          "schemaVersion": 1,
          "appName": "ReqLite",
          "exportedAt": 1000,
          "collections": [
            {
              "id": "c1",
              "name": "Body Types",
              "requests": [
                {
                  "id": "r1",
                  "collectionId": "c1",
                  "name": "No Body",
                  "method": "GET",
                  "url": "https://api.example.com",
                  "body": { "type": "NO_BODY" }
                },
                {
                  "id": "r2",
                  "collectionId": "c1",
                  "name": "Text Body",
                  "method": "POST",
                  "url": "https://api.example.com",
                  "body": { "type": "TEXT", "content": "hello world", "contentType": "text/plain" }
                },
                {
                  "id": "r3",
                  "collectionId": "c1",
                  "name": "Form Data",
                  "method": "POST",
                  "url": "https://api.example.com",
                  "body": {
                    "type": "FORM_DATA",
                    "fields": [{ "id": "f1", "key": "file", "value": "@image.png" }]
                  }
                }
              ]
            }
          ]
        }
        """.trimIndent()

        val result = manager.importWorkspace(jsonPayload, ImportStrategy.OVERWRITE_EXISTING)
        assertIs<WorkspaceImportResult.Success>(result)
        assertEquals(3, result.requestsImported)

        val r1 = fakeRequestRepo.getRequestById("r1")
        val r2 = fakeRequestRepo.getRequestById("r2")
        val r3 = fakeRequestRepo.getRequestById("r3")

        assertNotNull(r1)
        assertEquals(RequestBody.NoBody, r1.body)
        assertNotNull(r2)
        assertIs<RequestBody.TextBody>(r2.body)
        assertEquals("hello world", r2.body.content)
        assertNotNull(r3)
        assertIs<RequestBody.FormDataBody>(r3.body)
        assertEquals("file", r3.body.parts.first().key)
    }

    @Test
    fun importWorkspace_returns_error_on_invalid_json_or_blank_input() = runTest {
        assertTrue(manager.importWorkspace("") is WorkspaceImportResult.Error)
        assertTrue(manager.importWorkspace("   ") is WorkspaceImportResult.Error)
        assertTrue(manager.importWorkspace("{ not valid json }") is WorkspaceImportResult.Error)
    }

    @Test
    fun importWorkspace_records_warning_on_newer_schemaVersion() = runTest {
        val jsonPayload = """
        {
          "schemaVersion": 2,
          "appName": "ReqLite",
          "exportedAt": 1000
        }
        """.trimIndent()

        val result = manager.importWorkspace(jsonPayload)
        assertIs<WorkspaceImportResult.Success>(result)
        assertTrue(result.warnings.any { it.contains("newer than current") })
    }

    // Fakes
    class FakeCollectionRepository : CollectionRepository {
        private val collections = MutableStateFlow<List<Collection>>(emptyList())

        override suspend fun insertCollection(collection: Collection) {
            collections.value = collections.value + collection
        }

        override suspend fun updateCollection(collection: Collection) {
            collections.value = collections.value.map { if (it.id == collection.id) collection else it }
        }

        override suspend fun deleteCollection(collection: Collection) {
            collections.value = collections.value.filter { it.id != collection.id }
        }

        override fun getAllCollections(): Flow<List<Collection>> = collections.asStateFlow()
        override suspend fun getCollectionById(id: String): Collection? = collections.value.find { it.id == id }
    }

    class FakeRequestRepository : RequestRepository {
        private val requests = MutableStateFlow<List<Request>>(emptyList())

        override suspend fun insertRequest(request: Request) {
            requests.value = requests.value.filter { it.id != request.id } + request
        }

        override suspend fun getRequestById(id: String): Request? = requests.value.find { it.id == id }
        override fun getAllRequests(): Flow<List<Request>> = requests.asStateFlow()
        override suspend fun deleteRequest(id: String) {
            requests.value = requests.value.filter { it.id != id }
        }
        override suspend fun insertDraft(draft: Draft) {}
        override suspend fun getDraftById(id: String): Draft? = null
        override suspend fun getDraftForRequest(requestId: String): Draft? = null
        override fun getAllDrafts(): Flow<List<Draft>> = MutableStateFlow<List<Draft>>(emptyList()).asStateFlow()
        override suspend fun deleteDraft(id: String) {}
    }

    class FakeEnvironmentRepository : EnvironmentRepository {
        private val environments = MutableStateFlow<List<Environment>>(emptyList())

        override suspend fun insertEnvironment(environment: Environment) {
            environments.value = environments.value + environment
        }

        override suspend fun updateEnvironment(environment: Environment) {
            environments.value = environments.value.map { if (it.id == environment.id) environment else it }
        }

        override suspend fun deleteEnvironment(id: String) {
            environments.value = environments.value.filter { it.id != id }
        }

        override fun getAllEnvironments(): Flow<List<Environment>> = environments.asStateFlow()
        override suspend fun getEnvironmentById(id: String): Environment? = environments.value.find { it.id == id }
    }
}
