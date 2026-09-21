package com.learn.reqlite.domain.parser

import com.learn.reqlite.domain.model.HttpMethod
import com.learn.reqlite.domain.model.RequestBody
import kotlin.test.*

class SmartPayloadParserTest {

    private val parser = SmartPayloadParser()

    @Test
    fun testParseEmptyInput() {
        val result = parser.parse("")
        assertTrue(result is SmartPayload.Error)
    }

    @Test
    fun testParseCurlCommand() {
        val curl = "curl -X POST https://api.example.com/items -H 'Authorization: Bearer my_token' -H 'Content-Type: application/json' -d '{\"key\":\"value\"}'"
        val result = parser.parse(curl)
        assertTrue(result is SmartPayload.RequestConfig)
        assertEquals("https://api.example.com/items", result.url)
        assertEquals(HttpMethod.POST, result.method)
        assertEquals("my_token", result.bearerToken)
        assertTrue(result.body is RequestBody.TextBody)
        assertEquals("{\"key\":\"value\"}", (result.body as RequestBody.TextBody).content)
        assertEquals(1, result.headers.size)
        assertEquals("Content-Type", result.headers[0].key)
    }

    @Test
    fun testParseJsonEndpointConfig() {
        val jsonConfig = """
            {
                "url": "https://api.example.com/v1/users",
                "method": "POST",
                "headers": {
                    "Accept": "application/json",
                    "Authorization": "Bearer secret_123"
                },
                "body": "{\"name\":\"John Doe\"}"
            }
        """.trimIndent()
        val result = parser.parse(jsonConfig)
        assertTrue(result is SmartPayload.RequestConfig)
        assertEquals("https://api.example.com/v1/users", result.url)
        assertEquals(HttpMethod.POST, result.method)
        assertEquals("secret_123", result.bearerToken)
        assertEquals(1, result.headers.size)
        assertEquals("Accept", result.headers[0].key)
        assertTrue(result.body is RequestBody.TextBody)
    }

    @Test
    fun testParseJwtToken() {
        val jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        val result = parser.parse(jwt)
        assertTrue(result is SmartPayload.AuthToken)
        assertEquals(jwt, result.token)
        assertEquals("Bearer", result.type)
    }

    @Test
    fun testParseBearerTokenString() {
        val bearer = "Bearer my_access_token_abc"
        val result = parser.parse(bearer)
        assertTrue(result is SmartPayload.AuthToken)
        assertEquals("my_access_token_abc", result.token)
    }

    @Test
    fun testParsePlainUrl() {
        val url = "https://jsonplaceholder.typicode.com/todos/1"
        val result = parser.parse(url)
        assertTrue(result is SmartPayload.PlainUrl)
        assertEquals(url, result.url)
    }
}
