package com.learn.reqlite.domain.snippet

import com.learn.reqlite.domain.model.AuthConfiguration
import com.learn.reqlite.domain.model.HttpMethod
import com.learn.reqlite.domain.model.RequestBody
import com.learn.reqlite.domain.model.RequestField
import kotlin.test.*

class CodeSnippetGeneratorTest {

    private val generator: CodeSnippetGenerator = CodeSnippetGeneratorImpl()

    @Test
    fun testSupportedLanguagesList() {
        val languages = generator.getSupportedLanguages()
        assertEquals(11, languages.size)
        assertTrue(languages.contains(CodeSnippetLanguage.CURL))
        assertTrue(languages.contains(CodeSnippetLanguage.KOTLIN_KTOR))
        assertTrue(languages.contains(CodeSnippetLanguage.SWIFT_URLSESSION))
        assertTrue(languages.contains(CodeSnippetLanguage.JAVASCRIPT_FETCH))
        assertTrue(languages.contains(CodeSnippetLanguage.PYTHON_REQUESTS))
    }

    @Test
    fun testCurlGetWithQueryParamsAndAuth() {
        val request = CodeSnippetRequest(
            method = HttpMethod.GET,
            url = "https://api.example.com/v1/users",
            queryParams = listOf(
                RequestField(id = "1", key = "page", value = "2"),
                RequestField(id = "2", key = "limit", value = "50"),
                RequestField(id = "3", key = "disabled", value = "ignore", isEnabled = false)
            ),
            headers = listOf(
                RequestField(id = "4", key = "Accept", value = "application/json")
            ),
            auth = AuthConfiguration.Bearer("my_secret_token")
        )

        val snippet = generator.generateSnippet(request, CodeSnippetLanguage.CURL)

        assertTrue(snippet.contains("curl -X GET \"https://api.example.com/v1/users?page=2&limit=50\""))
        assertTrue(snippet.contains("-H \"Accept: application/json\""))
        assertTrue(snippet.contains("-H \"Authorization: Bearer my_secret_token\""))
        assertFalse(snippet.contains("disabled"))
    }

    @Test
    fun testCurlPostWithJsonBodyAndBasicAuth() {
        val request = CodeSnippetRequest(
            method = HttpMethod.POST,
            url = "https://api.example.com/auth/login",
            headers = listOf(
                RequestField(id = "1", key = "X-Api-Version", value = "2.0")
            ),
            body = RequestBody.TextBody("{\"username\":\"john\",\"remember\":true}", "application/json"),
            auth = AuthConfiguration.Basic("admin", "pass123")
        )

        val snippet = generator.generateSnippet(request, CodeSnippetLanguage.CURL)

        assertTrue(snippet.contains("-X POST"))
        assertTrue(snippet.contains("-u \"admin:pass123\""))
        assertTrue(snippet.contains("-H \"X-Api-Version: 2.0\""))
        assertTrue(snippet.contains("-H \"Content-Type: application/json\""))
        assertTrue(snippet.contains("-d '{\"username\":\"john\",\"remember\":true}'"))
    }

    @Test
    fun testKotlinKtorPost() {
        val request = CodeSnippetRequest(
            method = HttpMethod.POST,
            url = "https://api.example.com/orders",
            headers = listOf(
                RequestField(id = "1", key = "Authorization", value = "Bearer token_xyz")
            ),
            body = RequestBody.TextBody("{\"item\":\"Book\",\"qty\":1}", "application/json")
        )

        val snippet = generator.generateSnippet(request, CodeSnippetLanguage.KOTLIN_KTOR)

        assertTrue(snippet.contains("import io.ktor.client.*"))
        assertTrue(snippet.contains("method = HttpMethod.Post"))
        assertTrue(snippet.contains("client.request(\"https://api.example.com/orders\")"))
        assertTrue(snippet.contains("append(\"Authorization\", \"Bearer token_xyz\")"))
        assertTrue(snippet.contains("setBody(\"{\\\"item\\\":\\\"Book\\\",\\\"qty\\\":1}\")"))
    }

    @Test
    fun testKotlinOkHttpPost() {
        val request = CodeSnippetRequest(
            method = HttpMethod.POST,
            url = "https://api.example.com/items",
            body = RequestBody.TextBody("{\"title\":\"Test\"}", "application/json")
        )

        val snippet = generator.generateSnippet(request, CodeSnippetLanguage.KOTLIN_OKHTTP)

        assertTrue(snippet.contains("import okhttp3.*"))
        assertTrue(snippet.contains(".post(requestBody)"))
        assertTrue(snippet.contains(".url(\"https://api.example.com/items\")"))
    }

    @Test
    fun testSwiftUrlSessionPost() {
        val request = CodeSnippetRequest(
            method = HttpMethod.POST,
            url = "https://api.example.com/v1/create",
            headers = listOf(
                RequestField(id = "1", key = "X-Device", value = "iOS")
            ),
            body = RequestBody.TextBody("{\"name\":\"Device\"}", "application/json")
        )

        val snippet = generator.generateSnippet(request, CodeSnippetLanguage.SWIFT_URLSESSION)

        assertTrue(snippet.contains("import Foundation"))
        assertTrue(snippet.contains("request.httpMethod = \"POST\""))
        assertTrue(snippet.contains("request.setValue(\"iOS\", forHTTPHeaderField: \"X-Device\")"))
        assertTrue(snippet.contains("request.httpBody = \"{\\\"name\\\":\\\"Device\\\"}\".data(using: .utf8)"))
    }

    @Test
    fun testSwiftAlamofire() {
        val request = CodeSnippetRequest(
            method = HttpMethod.DELETE,
            url = "https://api.example.com/users/42"
        )

        val snippet = generator.generateSnippet(request, CodeSnippetLanguage.SWIFT_ALAMOFIRE)

        assertTrue(snippet.contains("import Alamofire"))
        assertTrue(snippet.contains("AF.request"))
        assertTrue(snippet.contains("method: .delete"))
    }

    @Test
    fun testJsFetchAndAxios() {
        val request = CodeSnippetRequest(
            method = HttpMethod.PUT,
            url = "https://api.example.com/profile",
            body = RequestBody.TextBody("{\"bio\":\"Developer\"}", "application/json")
        )

        val fetchSnippet = generator.generateSnippet(request, CodeSnippetLanguage.JAVASCRIPT_FETCH)
        assertTrue(fetchSnippet.contains("method: \"PUT\""))
        assertTrue(fetchSnippet.contains("body: `{\"bio\":\"Developer\"}`"))

        val axiosSnippet = generator.generateSnippet(request, CodeSnippetLanguage.JAVASCRIPT_AXIOS)
        assertTrue(axiosSnippet.contains("method: 'put'"))
        assertTrue(axiosSnippet.contains("data: `{\"bio\":\"Developer\"}`"))
    }

    @Test
    fun testPythonRequestsAndHttpx() {
        val request = CodeSnippetRequest(
            method = HttpMethod.POST,
            url = "https://api.example.com/ai/generate",
            body = RequestBody.TextBody("{\"prompt\":\"hello\"}", "application/json")
        )

        val reqSnippet = generator.generateSnippet(request, CodeSnippetLanguage.PYTHON_REQUESTS)
        assertTrue(reqSnippet.contains("import requests"))
        assertTrue(reqSnippet.contains("requests.request("))
        assertTrue(reqSnippet.contains("data=\"\"\"{\"prompt\":\"hello\"}\"\"\""))

        val httpxSnippet = generator.generateSnippet(request, CodeSnippetLanguage.PYTHON_HTTPX)
        assertTrue(httpxSnippet.contains("import httpx"))
        assertTrue(httpxSnippet.contains("with httpx.Client() as client:"))
    }

    @Test
    fun testGoNetHttp() {
        val request = CodeSnippetRequest(
            method = HttpMethod.POST,
            url = "https://api.example.com/data",
            body = RequestBody.TextBody("raw_payload_data", "text/plain")
        )

        val snippet = generator.generateSnippet(request, CodeSnippetLanguage.GO_NETHTTP)

        assertTrue(snippet.contains("package main"))
        assertTrue(snippet.contains("http.NewRequest(\"POST\", url, payload)"))
        assertTrue(snippet.contains("raw_payload_data"))
    }

    @Test
    fun testDartHttp() {
        val request = CodeSnippetRequest(
            method = HttpMethod.GET,
            url = "https://api.example.com/feed"
        )

        val snippet = generator.generateSnippet(request, CodeSnippetLanguage.DART_HTTP)

        assertTrue(snippet.contains("import 'package:http/http.dart' as http;"))
        assertTrue(snippet.contains("http.get("))
    }

    @Test
    fun testGraphQLBodyHandling() {
        val request = CodeSnippetRequest(
            method = HttpMethod.POST,
            url = "https://api.example.com/graphql",
            body = RequestBody.GraphQLBody(
                query = "query GetUser { user { id name } }",
                variables = "{\"id\":\"123\"}"
            )
        )

        val snippet = generator.generateSnippet(request, CodeSnippetLanguage.CURL)

        assertTrue(snippet.contains("query GetUser"))
        assertTrue(snippet.contains("-H \"Content-Type: application/json\""))
    }
}
