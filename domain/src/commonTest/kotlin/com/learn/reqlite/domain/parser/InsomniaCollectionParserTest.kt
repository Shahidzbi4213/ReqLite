package com.learn.reqlite.domain.parser

import com.learn.reqlite.domain.model.HttpMethod
import com.learn.reqlite.domain.model.RequestBody
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InsomniaCollectionParserTest {

    private val parser = InsomniaCollectionParserImpl()

    @Test
    fun parse_validInsomniaV4Bundle_extractsAllEntities() {
        val json = """
        {
          "_type": "export",
          "__export_format": 4,
          "__export_date": "2026-01-01T00:00:00.000Z",
          "__export_source": "insomnia.desktop.app:v2023.5.8",
          "resources": [
            {
              "_id": "wrk_1",
              "parentId": null,
              "created": 1600000000000,
              "modified": 1600000000000,
              "name": "E-Commerce API",
              "description": "Insomnia Workspace for testing",
              "_type": "workspace"
            },
            {
              "_id": "fld_1",
              "parentId": "wrk_1",
              "name": "Auth",
              "description": "Authentication endpoints",
              "_type": "request_group"
            },
            {
              "_id": "fld_2",
              "parentId": "fld_1",
              "name": "OAuth",
              "description": "Nested folder",
              "_type": "request_group"
            },
            {
              "_id": "req_login",
              "parentId": "fld_1",
              "name": "User Login",
              "url": "{{ _.baseUrl }}/auth/login",
              "method": "POST",
              "headers": [
                {
                  "name": "Content-Type",
                  "value": "application/json"
                }
              ],
              "parameters": [
                {
                  "name": "redirect",
                  "value": "true"
                }
              ],
              "body": {
                "mimeType": "application/json",
                "text": "{\"user\":\"admin\",\"pass\":\"secret\"}"
              },
              "authentication": {
                "type": "bearer",
                "token": "{{ _.apiToken }}",
                "prefix": "Bearer"
              },
              "_type": "request"
            },
            {
              "_id": "req_graphql",
              "parentId": "wrk_1",
              "name": "GraphQL Query",
              "url": "{{baseUrl}}/graphql",
              "method": "POST",
              "body": {
                "mimeType": "application/graphql",
                "text": "{\"query\": \"query GetUsers { users { id name } }\", \"variables\": \"{}\"}"
              },
              "_type": "request"
            },
            {
              "_id": "req_form",
              "parentId": "wrk_1",
              "name": "Form Post",
              "url": "https://api.example.com/upload",
              "method": "POST",
              "body": {
                "mimeType": "application/x-www-form-urlencoded",
                "params": [
                  { "name": "field1", "value": "val1" },
                  { "name": "disabledField", "value": "val2", "disabled": true }
                ]
              },
              "_type": "request"
            },
            {
              "_id": "env_base",
              "parentId": "wrk_1",
              "name": "Production",
              "color": "#1288ee",
              "data": {
                "baseUrl": "https://api.example.com",
                "apiToken": "secret_tok_123"
              },
              "isPrivate": true,
              "_type": "environment"
            }
          ]
        }
        """.trimIndent()

        val result = parser.parse(json)
        assertIs<InsomniaParseResult.Success>(result)

        // Workspace
        assertEquals("wrk_1", result.collection.id)
        assertEquals("E-Commerce API", result.collection.name)
        assertEquals("Insomnia Workspace for testing", result.collection.description)

        // Folders
        assertEquals(2, result.folders.size)
        val authFolder = result.folders.find { it.id == "fld_1" }
        assertNotNull(authFolder)
        assertEquals("Auth", authFolder.name)
        assertEquals(null, authFolder.parentFolderId)

        val oauthFolder = result.folders.find { it.id == "fld_2" }
        assertNotNull(oauthFolder)
        assertEquals("OAuth", oauthFolder.name)
        assertEquals("fld_1", oauthFolder.parentFolderId)

        // Requests
        assertEquals(3, result.requests.size)

        val loginReq = result.requests.find { it.id == "req_login" }
        assertNotNull(loginReq)
        assertEquals("User Login", loginReq.name)
        assertEquals(HttpMethod.POST, loginReq.method)
        assertEquals("{{baseUrl}}/auth/login", loginReq.url)
        assertEquals("fld_1", loginReq.folderId)
        assertEquals(1, loginReq.queryParams.size)
        assertEquals("redirect", loginReq.queryParams[0].key)

        // Headers include Content-Type + Injected Bearer Auth
        assertTrue(loginReq.headers.any { it.key == "Content-Type" && it.value == "application/json" })
        assertTrue(loginReq.headers.any { it.key == "Authorization" && it.value == "Bearer {{apiToken}}" })

        val loginBody = loginReq.body
        assertIs<RequestBody.TextBody>(loginBody)
        assertEquals("{\"user\":\"admin\",\"pass\":\"secret\"}", loginBody.content)

        // GraphQL Request
        val gqlReq = result.requests.find { it.id == "req_graphql" }
        assertNotNull(gqlReq)
        val gqlBody = gqlReq.body
        assertIs<RequestBody.GraphQLBody>(gqlBody)
        assertTrue(gqlBody.query.contains("query GetUsers"))

        // Form Request
        val formReq = result.requests.find { it.id == "req_form" }
        assertNotNull(formReq)
        val formBody = formReq.body
        assertIs<RequestBody.UrlEncodedBody>(formBody)
        assertEquals(1, formBody.fields.filter { it.isEnabled }.size)
        assertEquals("field1", formBody.fields[0].key)

        // Environments
        assertEquals(1, result.environments.size)
        val env = result.environments[0]
        assertEquals("Production", env.name)
        assertEquals("#1288ee", env.color)
        assertEquals(2, env.variables.size)
        assertTrue(env.variables.any { it.key == "baseUrl" && it.value == "https://api.example.com" })
        assertTrue(env.variables.any { it.key == "apiToken" && it.isSecret })
    }

    @Test
    fun parse_basicAuth_injectsAuthorizationHeader() {
        val json = """
        {
          "__export_format": 4,
          "resources": [
            {
              "_id": "req_basic",
              "name": "Basic Auth Req",
              "url": "https://httpbin.org/basic-auth/user/passwd",
              "method": "GET",
              "authentication": {
                "type": "basic",
                "username": "user",
                "password": "passwd"
              },
              "_type": "request"
            }
          ]
        }
        """.trimIndent()

        val result = parser.parse(json)
        assertIs<InsomniaParseResult.Success>(result)
        val req = result.requests.first()
        val authHeader = req.headers.find { it.key == "Authorization" }
        assertNotNull(authHeader)
        // Base64 of "user:passwd" is "dXNlcjpwYXNzd2Q="
        assertEquals("Basic dXNlcjpwYXNzd2Q=", authHeader.value)
    }

    @Test
    fun parse_emptyInput_returnsError() {
        val result = parser.parse("")
        assertIs<InsomniaParseResult.Error>(result)
    }

    @Test
    fun parse_invalidJson_returnsError() {
        val result = parser.parse("{ invalid json")
        assertIs<InsomniaParseResult.Error>(result)
    }

    @Test
    fun parse_missingResources_returnsError() {
        val result = parser.parse("""{ "foo": "bar" }""")
        assertIs<InsomniaParseResult.Error>(result)
    }
}
