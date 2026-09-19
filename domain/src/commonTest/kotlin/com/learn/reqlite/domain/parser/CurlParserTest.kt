package com.learn.reqlite.domain.parser

import com.learn.reqlite.domain.model.HttpMethod
import com.learn.reqlite.domain.model.RequestBody
import kotlin.test.*

class CurlParserTest {

    private val parser: CurlParser = CurlParserImpl()

    @Test
    fun parse_simpleGetRequest() {
        val result = parser.parse("curl https://api.example.com/users")
        assertIs<CurlParseResult.Success>(result)

        val draft = result.draft
        assertEquals(HttpMethod.GET, draft.method)
        assertEquals("https://api.example.com/users", draft.url)
        assertEquals(RequestBody.NoBody, draft.body)
        assertTrue(draft.headers.isEmpty())
        assertTrue(draft.queryParams.isEmpty())
    }

    @Test
    fun parse_customMethodAndHeaders() {
        val cmd = "curl -X DELETE -H 'Authorization: Bearer token123' -H \"X-Custom: Value\" https://api.example.com/item/42"
        val result = parser.parse(cmd)
        assertIs<CurlParseResult.Success>(result)

        val draft = result.draft
        assertEquals(HttpMethod.DELETE, draft.method)
        assertEquals("https://api.example.com/item/42", draft.url)
        assertEquals(2, draft.headers.size)
        assertEquals("Authorization", draft.headers[0].key)
        assertEquals("Bearer token123", draft.headers[0].value)
        assertEquals("X-Custom", draft.headers[1].key)
        assertEquals("Value", draft.headers[1].value)
    }

    @Test
    fun parse_jsonBodyWithPost() {
        val cmd = """curl -X POST https://api.example.com/create -H "Content-Type: application/json" -d '{"name":"test","count":10}'"""
        val result = parser.parse(cmd)
        assertIs<CurlParseResult.Success>(result)

        val draft = result.draft
        assertEquals(HttpMethod.POST, draft.method)
        assertEquals("https://api.example.com/create", draft.url)
        assertIs<RequestBody.TextBody>(draft.body)

        val textBody = draft.body
        assertEquals("""{"name":"test","count":10}""", textBody.content)
        assertEquals("application/json", textBody.contentType)
    }

    @Test
    fun parse_multilineCommandWithBackslashes() {
        val cmd = """
            curl --location 'https://api.example.com/items' \
              --header 'Content-Type: application/json' \
              --header 'Accept: application/json' \
              --data-raw '{
                "title": "Item Title",
                "enabled": true
              }'
        """.trimIndent()

        val result = parser.parse(cmd)
        assertIs<CurlParseResult.Success>(result)

        val draft = result.draft
        assertEquals(HttpMethod.POST, draft.method)
        assertEquals("https://api.example.com/items", draft.url)
        assertEquals(2, draft.headers.size)
        assertIs<RequestBody.TextBody>(draft.body)
        assertTrue(draft.body.content.contains("Item Title"))
    }

    @Test
    fun parse_urlQueryParamsExtracted() {
        val cmd = "curl 'https://api.example.com/search?q=kotlin&page=2&sort=asc'"
        val result = parser.parse(cmd)
        assertIs<CurlParseResult.Success>(result)

        val draft = result.draft
        assertEquals("https://api.example.com/search", draft.url)
        assertEquals(3, draft.queryParams.size)
        assertEquals("q", draft.queryParams[0].key)
        assertEquals("kotlin", draft.queryParams[0].value)
        assertEquals("page", draft.queryParams[1].key)
        assertEquals("2", draft.queryParams[1].value)
        assertEquals("sort", draft.queryParams[2].key)
        assertEquals("asc", draft.queryParams[2].value)
    }

    @Test
    fun parse_basicAuthFlag() {
        val cmd = "curl -u admin:secret123 https://api.example.com/admin"
        val result = parser.parse(cmd)
        assertIs<CurlParseResult.Success>(result)

        val draft = result.draft
        val authHeader = draft.headers.find { it.key == "Authorization" }
        assertNotNull(authHeader)
        // admin:secret123 in base64 is YWRtaW46c2VjcmV0MTIz
        assertEquals("Basic YWRtaW46c2VjcmV0MTIz", authHeader.value)
    }

    @Test
    fun parse_formDataBody() {
        val cmd = "curl -X POST https://api.example.com/upload -F 'username=johndoe' -F 'file=@photo.jpg'"
        val result = parser.parse(cmd)
        assertIs<CurlParseResult.Success>(result)

        val draft = result.draft
        assertIs<RequestBody.FormDataBody>(draft.body)
        val formBody = draft.body
        assertEquals(2, formBody.parts.size)
        assertEquals("username", formBody.parts[0].key)
        assertEquals("johndoe", formBody.parts[0].value)
        assertEquals("file", formBody.parts[1].key)
        assertEquals("@photo.jpg", formBody.parts[1].value)
    }

    @Test
    fun parse_urlEncodedBody() {
        val cmd = "curl -X POST https://api.example.com/oauth/token -H 'Content-Type: application/x-www-form-urlencoded' -d 'grant_type=client_credentials&client_id=abc'"
        val result = parser.parse(cmd)
        assertIs<CurlParseResult.Success>(result)

        val draft = result.draft
        assertIs<RequestBody.UrlEncodedBody>(draft.body)
        val urlBody = draft.body
        assertEquals(2, urlBody.fields.size)
        assertEquals("grant_type", urlBody.fields[0].key)
        assertEquals("client_credentials", urlBody.fields[0].value)
        assertEquals("client_id", urlBody.fields[1].key)
        assertEquals("abc", urlBody.fields[1].value)
    }

    @Test
    fun parse_implicitPostOnData() {
        val cmd = "curl https://api.example.com/data -d 'simple payload'"
        val result = parser.parse(cmd)
        assertIs<CurlParseResult.Success>(result)

        val draft = result.draft
        assertEquals(HttpMethod.POST, draft.method)
        assertIs<RequestBody.TextBody>(draft.body)
        assertEquals("simple payload", draft.body.content)
    }

    @Test
    fun parse_getWithData_convertsDataToQueryParams() {
        val cmd = "curl -G https://api.example.com/users -d 'role=admin' -d 'active=true'"
        val result = parser.parse(cmd)
        assertIs<CurlParseResult.Success>(result)

        val draft = result.draft
        assertEquals(HttpMethod.GET, draft.method)
        assertEquals(RequestBody.NoBody, draft.body)
        assertEquals(2, draft.queryParams.size)
        assertEquals("role", draft.queryParams[0].key)
        assertEquals("admin", draft.queryParams[0].value)
        assertEquals("active", draft.queryParams[1].key)
        assertEquals("true", draft.queryParams[1].value)
    }

    @Test
    fun parse_userAgentAndCookies() {
        val cmd = "curl -A 'CustomAgent/1.0' -b 'session=xyz123' https://api.example.com"
        val result = parser.parse(cmd)
        assertIs<CurlParseResult.Success>(result)

        val draft = result.draft
        val ua = draft.headers.find { it.key == "User-Agent" }
        val cookie = draft.headers.find { it.key == "Cookie" }

        assertNotNull(ua)
        assertEquals("CustomAgent/1.0", ua.value)
        assertNotNull(cookie)
        assertEquals("session=xyz123", cookie.value)
    }

    @Test
    fun parse_multipleDataFlags_concatenatesWithAmpersand() {
        val cmd = "curl https://api.example.com/submit -d 'first=1' -d 'second=2'"
        val result = parser.parse(cmd)
        assertIs<CurlParseResult.Success>(result)

        val draft = result.draft
        assertIs<RequestBody.TextBody>(draft.body)
        assertEquals("first=1&second=2", draft.body.content)
        assertEquals("application/x-www-form-urlencoded", draft.body.contentType)
    }

    @Test
    fun parse_emptyAndInvalidCommands_returnsError() {
        assertTrue(parser.parse("") is CurlParseResult.Error)
        assertTrue(parser.parse("   ") is CurlParseResult.Error)
        assertTrue(parser.parse("wget https://example.com") is CurlParseResult.Error)
        assertTrue(parser.parse("curl") is CurlParseResult.Error)
    }
}
