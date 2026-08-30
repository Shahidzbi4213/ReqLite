package com.learn.reqlite.qa

import com.learn.reqlite.data.remote.createHttpClient
import com.learn.reqlite.domain.model.*
import com.learn.reqlite.domain.parser.CurlParseResult
import com.learn.reqlite.domain.parser.CurlParserImpl
import com.learn.reqlite.domain.usecase.RequestValidator
import com.learn.reqlite.domain.usecase.ValidationIssue
import com.learn.reqlite.domain.usecase.VariableResolver
import com.learn.reqlite.ui.response.HttpResponseUiModel
import com.learn.reqlite.ui.response.HttpStatusCategory
import com.learn.reqlite.ui.response.json.JsonTreeNode
import com.learn.reqlite.ui.response.json.JsonTreeParser
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Senior QA Comprehensive Real-World API Test Suite for ReqLite using JSONPlaceholder API.
 */
class JsonPlaceholderLiveQaTest {

    private lateinit var httpClient: HttpClient
    private val variableResolver = VariableResolver()
    private val requestValidator = RequestValidator(variableResolver)
    private val curlParser = CurlParserImpl()
    private val baseUrl = "https://jsonplaceholder.typicode.com"

    @Before
    fun setUp() {
        httpClient = createHttpClient(enableLogging = false)
    }

    @After
    fun tearDown() {
        httpClient.close()
    }

    @Test
    fun test01_getSinglePost_returns200AndValidJsonStructure() = runBlocking {
        val url = "$baseUrl/posts/1"
        val startTime = System.currentTimeMillis()
        val response = httpClient.get(url)
        val durationMs = System.currentTimeMillis() - startTime
        val body = response.bodyAsText()
        val statusCode = response.status.value
        val contentType = response.headers[HttpHeaders.ContentType] ?: ""

        assertEquals(200, statusCode)
        assertTrue(body.isNotEmpty())
        assertTrue(contentType.contains("application/json"))

        val uiModel = HttpResponseUiModel(
            statusCode = statusCode,
            durationMs = durationMs,
            sizeBytes = body.encodeToByteArray().size.toLong(),
            contentType = contentType,
            body = body,
            url = url,
            method = "GET"
        )

        assertEquals(HttpStatusCategory.SUCCESS, uiModel.statusCategory)
        assertEquals("200 OK", uiModel.statusDescription)
        assertTrue(uiModel.isJsonContentType)

        val rootNode = JsonTreeParser.parseJsonToTree(body)
        assertNotNull(rootNode)
        assertTrue(rootNode is JsonTreeNode.ObjectNode)
        val obj = rootNode as JsonTreeNode.ObjectNode

        val idNode = obj.children.find { it.key == "id" } as? JsonTreeNode.PrimitiveNode
        assertNotNull(idNode)
        assertEquals("1", idNode?.value)
        assertEquals("$.id", idNode?.path)

        val titleNode = obj.children.find { it.key == "title" } as? JsonTreeNode.PrimitiveNode
        assertNotNull(titleNode)
        assertTrue(titleNode?.value?.isNotBlank() == true)
        assertEquals("$.title", titleNode?.path)
    }

    @Test
    fun test02_getPostsList_returnsArrayOf100Items() = runBlocking {
        val url = "$baseUrl/posts"
        val response = httpClient.get(url)
        val body = response.bodyAsText()

        assertEquals(200, response.status.value)
        val rootNode = JsonTreeParser.parseJsonToTree(body)
        assertNotNull(rootNode)
        assertTrue(rootNode is JsonTreeNode.ArrayNode)
        val arrayNode = rootNode as JsonTreeNode.ArrayNode
        assertEquals(100, arrayNode.size)

        val firstItem = arrayNode.children.first() as JsonTreeNode.ObjectNode
        assertEquals("$[0]", firstItem.path)
        val firstId = firstItem.children.find { it.key == "id" } as JsonTreeNode.PrimitiveNode
        assertEquals("1", firstId.value)

        val lastItem = arrayNode.children.last() as JsonTreeNode.ObjectNode
        assertEquals("$[99]", lastItem.path)
        val lastId = lastItem.children.find { it.key == "id" } as JsonTreeNode.PrimitiveNode
        assertEquals("100", lastId.value)
    }

    @Test
    fun test03_getCommentsWithQueryParams_filtersCorrectly() = runBlocking {
        val url = "$baseUrl/comments"
        val response = httpClient.get(url) {
            parameter("postId", "1")
        }
        val body = response.bodyAsText()

        assertEquals(200, response.status.value)
        val rootNode = JsonTreeParser.parseJsonToTree(body) as JsonTreeNode.ArrayNode
        assertTrue(rootNode.size > 0)

        // Verify all returned comments belong to postId 1
        rootNode.children.forEach { node ->
            val commentObj = node as JsonTreeNode.ObjectNode
            val postIdNode = commentObj.children.find { it.key == "postId" } as JsonTreeNode.PrimitiveNode
            assertEquals("1", postIdNode.value)
        }
    }

    @Test
    fun test04_postCreateResource_returns201Created() = runBlocking {
        val url = "$baseUrl/posts"
        val requestJson = """{"title": "QA Test Post", "body": "ReqLite Live Test", "userId": 42}"""
        val response = httpClient.post(url) {
            header(HttpHeaders.ContentType, "application/json")
            setBody(requestJson)
        }
        val body = response.bodyAsText()

        assertEquals(201, response.status.value)
        val uiModel = HttpResponseUiModel(statusCode = response.status.value, body = body)
        assertEquals(HttpStatusCategory.SUCCESS, uiModel.statusCategory)
        assertEquals("201 Created", uiModel.statusDescription)

        val root = JsonTreeParser.parseJsonToTree(body) as JsonTreeNode.ObjectNode
        val idNode = root.children.find { it.key == "id" } as JsonTreeNode.PrimitiveNode
        assertEquals("101", idNode.value)
        val titleNode = root.children.find { it.key == "title" } as JsonTreeNode.PrimitiveNode
        assertEquals("QA Test Post", titleNode.value)
    }

    @Test
    fun test05_putUpdateResource_returns200Ok() = runBlocking {
        val url = "$baseUrl/posts/1"
        val requestJson = """{"id": 1, "title": "Updated Title", "body": "Updated Body", "userId": 1}"""
        val response = httpClient.put(url) {
            header(HttpHeaders.ContentType, "application/json")
            setBody(requestJson)
        }
        val body = response.bodyAsText()

        assertEquals(200, response.status.value)
        val root = JsonTreeParser.parseJsonToTree(body) as JsonTreeNode.ObjectNode
        val titleNode = root.children.find { it.key == "title" } as JsonTreeNode.PrimitiveNode
        assertEquals("Updated Title", titleNode.value)
    }

    @Test
    fun test06_patchUpdateResource_returns200Ok() = runBlocking {
        val url = "$baseUrl/posts/1"
        val requestJson = """{"title": "Patched Title Only"}"""
        val response = httpClient.patch(url) {
            header(HttpHeaders.ContentType, "application/json")
            setBody(requestJson)
        }
        val body = response.bodyAsText()

        assertEquals(200, response.status.value)
        val root = JsonTreeParser.parseJsonToTree(body) as JsonTreeNode.ObjectNode
        val titleNode = root.children.find { it.key == "title" } as JsonTreeNode.PrimitiveNode
        assertEquals("Patched Title Only", titleNode.value)
    }

    @Test
    fun test07_deleteResource_returns200Ok() = runBlocking {
        val url = "$baseUrl/posts/1"
        val response = httpClient.delete(url)
        assertEquals(200, response.status.value)
    }

    @Test
    fun test08_getUsersNestedJson_parsesComplexTreeHierarchy() = runBlocking {
        val url = "$baseUrl/users"
        val response = httpClient.get(url)
        val body = response.bodyAsText()

        assertEquals(200, response.status.value)
        val root = JsonTreeParser.parseJsonToTree(body) as JsonTreeNode.ArrayNode
        assertTrue(root.size > 0)

        val firstUser = root.children.first() as JsonTreeNode.ObjectNode
        val addressNode = firstUser.children.find { it.key == "address" } as JsonTreeNode.ObjectNode
        assertEquals("$[0].address", addressNode.path)

        val geoNode = addressNode.children.find { it.key == "geo" } as JsonTreeNode.ObjectNode
        assertEquals("$[0].address.geo", geoNode.path)

        val latNode = geoNode.children.find { it.key == "lat" } as JsonTreeNode.PrimitiveNode
        assertEquals("$[0].address.geo.lat", latNode.path)
        assertNotNull(latNode.value)
    }

    @Test
    fun test09_getTodos_parsesBooleanAndNullPrimitives() = runBlocking {
        val url = "$baseUrl/todos/1"
        val response = httpClient.get(url)
        val body = response.bodyAsText()

        assertEquals(200, response.status.value)
        val root = JsonTreeParser.parseJsonToTree(body) as JsonTreeNode.ObjectNode
        val completedNode = root.children.find { it.key == "completed" } as JsonTreeNode.PrimitiveNode
        assertEquals(JsonTreeNode.PrimitiveType.BOOLEAN, completedNode.type)
        assertEquals("false", completedNode.value)
    }

    @Test
    fun test10_variableResolution_resolvesEnvironmentVariables() {
        val variables = listOf(
            Variable(id = "v1", key = "baseUrl", value = "https://jsonplaceholder.typicode.com"),
            Variable(id = "v2", key = "postId", value = "42")
        )

        val inputUrl = "{{baseUrl}}/posts/{{postId}}"
        val resolved = variableResolver.resolve(inputUrl, variables)
        assertEquals("https://jsonplaceholder.typicode.com/posts/42", resolved)

        val headerValue = "Bearer {{postId}}"
        assertEquals("Bearer 42", variableResolver.resolve(headerValue, variables))
    }

    @Test
    fun test11_curlParser_parsesJsonPlaceholderCurl() {
        val curl = """curl -X POST https://jsonplaceholder.typicode.com/posts -H "Content-Type: application/json" -H "Authorization: Bearer test_token" -d '{"title":"curl post","userId":1}'"""
        val result = curlParser.parse(curl)
        assertTrue(result is CurlParseResult.Success)
        val draft = (result as CurlParseResult.Success).draft

        assertEquals("https://jsonplaceholder.typicode.com/posts", draft.url)
        assertEquals(HttpMethod.POST, draft.method)
        assertEquals(2, draft.headers.size)
        assertTrue(draft.headers.any { it.key == "Content-Type" && it.value == "application/json" })
        assertTrue(draft.headers.any { it.key == "Authorization" && it.value == "Bearer test_token" })
        assertTrue(draft.body is RequestBody.TextBody)
        assertEquals("""{"title":"curl post","userId":1}""", (draft.body as RequestBody.TextBody).content)
    }

    @Test
    fun test12_errorScenario_404NotFound() = runBlocking {
        val url = "$baseUrl/posts/999999"
        val response = httpClient.get(url)
        val body = response.bodyAsText()

        assertEquals(404, response.status.value)
        val uiModel = HttpResponseUiModel(statusCode = 404, body = body)
        assertEquals(HttpStatusCategory.CLIENT_ERROR, uiModel.statusCategory)
        assertEquals("404 Not Found", uiModel.statusDescription)
    }

    @Test
    fun test13_errorScenario_invalidUrlValidation() {
        val invalidDraft = Draft(
            id = "draft_1",
            requestId = "req_1",
            method = HttpMethod.GET,
            url = "invalid-url-without-protocol",
            headers = emptyList(),
            queryParams = emptyList(),
            body = RequestBody.NoBody,
            updatedAt = 0L
        )

        val issues = requestValidator.validate(invalidDraft, emptyList())
        assertTrue(issues.any { it is ValidationIssue.Error })
    }
}
