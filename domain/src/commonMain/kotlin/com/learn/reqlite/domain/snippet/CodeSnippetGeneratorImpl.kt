package com.learn.reqlite.domain.snippet

import com.learn.reqlite.domain.model.AuthConfiguration
import com.learn.reqlite.domain.model.HttpMethod
import com.learn.reqlite.domain.model.RequestBody
import com.learn.reqlite.domain.model.RequestField
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CodeSnippetGeneratorImpl : CodeSnippetGenerator {

    @OptIn(ExperimentalEncodingApi::class)
    override fun generateSnippet(request: CodeSnippetRequest, language: CodeSnippetLanguage): String {
        val fullUrl = buildFullUrl(request.url, request.queryParams)
        val allHeaders = buildEffectiveHeaders(request)
        val bodyData = extractBodyData(request.body)

        return when (language) {
            CodeSnippetLanguage.CURL -> generateCurl(request.method, fullUrl, allHeaders, bodyData, request.auth)
            CodeSnippetLanguage.KOTLIN_KTOR -> generateKotlinKtor(request.method, fullUrl, allHeaders, bodyData)
            CodeSnippetLanguage.KOTLIN_OKHTTP -> generateKotlinOkHttp(request.method, fullUrl, allHeaders, bodyData)
            CodeSnippetLanguage.SWIFT_URLSESSION -> generateSwiftUrlSession(request.method, fullUrl, allHeaders, bodyData)
            CodeSnippetLanguage.SWIFT_ALAMOFIRE -> generateSwiftAlamofire(request.method, fullUrl, allHeaders, bodyData)
            CodeSnippetLanguage.JAVASCRIPT_FETCH -> generateJsFetch(request.method, fullUrl, allHeaders, bodyData)
            CodeSnippetLanguage.JAVASCRIPT_AXIOS -> generateJsAxios(request.method, fullUrl, allHeaders, bodyData)
            CodeSnippetLanguage.PYTHON_REQUESTS -> generatePythonRequests(request.method, fullUrl, allHeaders, bodyData)
            CodeSnippetLanguage.PYTHON_HTTPX -> generatePythonHttpx(request.method, fullUrl, allHeaders, bodyData)
            CodeSnippetLanguage.GO_NETHTTP -> generateGoNetHttp(request.method, fullUrl, allHeaders, bodyData)
            CodeSnippetLanguage.DART_HTTP -> generateDartHttp(request.method, fullUrl, allHeaders, bodyData)
        }
    }

    private data class ExtractedBody(
        val content: String?,
        val contentType: String?,
        val isFormUrlEncoded: Boolean = false,
        val isFormData: Boolean = false,
        val formFields: List<Pair<String, String>> = emptyList()
    )

    private fun extractBodyData(body: RequestBody): ExtractedBody {
        return when (body) {
            is RequestBody.NoBody -> ExtractedBody(null, null)
            is RequestBody.TextBody -> ExtractedBody(body.content, body.contentType)
            is RequestBody.UrlEncodedBody -> {
                val fields = body.fields.filter { it.isEnabled }.map { it.key to it.value }
                val encoded = fields.joinToString("&") { "${encodeQueryComponent(it.first)}=${encodeQueryComponent(it.second)}" }
                ExtractedBody(encoded, "application/x-www-form-urlencoded", isFormUrlEncoded = true, formFields = fields)
            }
            is RequestBody.FormDataBody -> {
                val parts = body.parts.filter { it.isEnabled }.map { it.key to it.value }
                ExtractedBody(null, "multipart/form-data", isFormData = true, formFields = parts)
            }
            is RequestBody.GraphQLBody -> {
                val jsonPayload = buildJsonObject {
                    put("query", body.query)
                    if (body.variables.isNotBlank()) {
                        try {
                            val parsedVars = Json.parseToJsonElement(body.variables)
                            put("variables", parsedVars)
                        } catch (_: Exception) {
                            put("variables", body.variables)
                        }
                    }
                    if (!body.operationName.isNullOrBlank()) {
                        put("operationName", body.operationName)
                    }
                }.toString()
                ExtractedBody(jsonPayload, "application/json")
            }
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun buildEffectiveHeaders(request: CodeSnippetRequest): Map<String, String> {
        val headersMap = mutableMapOf<String, String>()
        request.headers.filter { it.isEnabled && it.key.isNotBlank() }.forEach {
            headersMap[it.key] = it.value
        }

        // Add Auth headers if needed
        when (val auth = request.auth) {
            is AuthConfiguration.None -> {}
            is AuthConfiguration.Bearer -> {
                if (auth.token.isNotBlank()) {
                    headersMap["Authorization"] = "Bearer ${auth.token}"
                }
            }
            is AuthConfiguration.Basic -> {
                if (auth.username.isNotBlank() || auth.password.isNotBlank()) {
                    val creds = "${auth.username}:${auth.password}"
                    val encoded = Base64.encode(creds.encodeToByteArray())
                    headersMap["Authorization"] = "Basic $encoded"
                }
            }
        }

        // Add Content-Type if body demands it and not explicitly set
        val bodyData = extractBodyData(request.body)
        if (bodyData.contentType != null && !headersMap.keys.any { it.equals("Content-Type", ignoreCase = true) }) {
            headersMap["Content-Type"] = bodyData.contentType
        }

        return headersMap
    }

    private fun buildFullUrl(baseUrl: String, queryParams: List<RequestField>): String {
        val enabledParams = queryParams.filter { it.isEnabled && it.key.isNotBlank() }
        if (enabledParams.isEmpty()) return baseUrl

        val queryString = enabledParams.joinToString("&") {
            "${encodeQueryComponent(it.key)}=${encodeQueryComponent(it.value)}"
        }

        return when {
            baseUrl.contains("?") -> if (baseUrl.endsWith("?") || baseUrl.endsWith("&")) "$baseUrl$queryString" else "$baseUrl&$queryString"
            else -> "$baseUrl?$queryString"
        }
    }

    private fun encodeQueryComponent(value: String): String {
        return value.replace(" ", "%20")
            .replace("#", "%23")
            .replace("&", "%26")
            .replace("=", "%3D")
    }

    private fun escapeForKotlin(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("$", "\\$")
            .replace("\r", "")
            .replace("\n", "\\n")
    }

    private fun escapeForSwift(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "")
            .replace("\n", "\\n")
    }

    private fun escapeForJs(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "")
            .replace("\n", "\\n")
    }

    private fun escapeForJsTemplate(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("\${", "\\\${")
            .replace("\r", "")
    }

    private fun escapeForPython(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "")
            .replace("\n", "\\n")
    }

    // 1. cURL
    private fun generateCurl(
        method: HttpMethod,
        url: String,
        headers: Map<String, String>,
        body: ExtractedBody,
        auth: AuthConfiguration
    ): String {
        val lines = mutableListOf<String>()
        val safeUrl = if (url.isBlank()) "https://api.example.com" else url

        lines.add("curl -X ${method.name} \"$safeUrl\" \\")

        // In cURL, basic auth can use -u flag or Authorization header
        if (auth is AuthConfiguration.Basic && (auth.username.isNotBlank() || auth.password.isNotBlank())) {
            lines.add("  -u \"${auth.username}:${auth.password}\" \\")
        }

        headers.forEach { (k, v) ->
            if (auth is AuthConfiguration.Basic && k.equals("Authorization", ignoreCase = true)) {
                // skipped in favor of -u if basic
            } else {
                lines.add("  -H \"$k: $v\" \\")
            }
        }

        if (body.isFormData) {
            body.formFields.forEach { (k, v) ->
                lines.add("  -F \"$k=$v\" \\")
            }
        } else if (body.content != null && body.content.isNotBlank()) {
            val escapedBody = body.content.replace("'", "'\\''")
            lines.add("  -d '$escapedBody' \\")
        }

        val joined = lines.joinToString("\n")
        return joined.removeSuffix(" \\")
    }

    // 2. Kotlin (Ktor)
    private fun generateKotlinKtor(
        method: HttpMethod,
        url: String,
        headers: Map<String, String>,
        body: ExtractedBody
    ): String {
        val ktorMethod = when (method) {
            HttpMethod.GET -> "HttpMethod.Get"
            HttpMethod.POST -> "HttpMethod.Post"
            HttpMethod.PUT -> "HttpMethod.Put"
            HttpMethod.DELETE -> "HttpMethod.Delete"
            HttpMethod.PATCH -> "HttpMethod.Patch"
            HttpMethod.HEAD -> "HttpMethod.Head"
            HttpMethod.OPTIONS -> "HttpMethod.Options"
        }

        val headerLines = if (headers.isNotEmpty()) {
            buildString {
                appendLine("        headers {")
                headers.forEach { (k, v) ->
                    appendLine("            append(\"${escapeForKotlin(k)}\", \"${escapeForKotlin(v)}\")")
                }
                append("        }")
            }
        } else ""

        val bodyLine = if (body.content != null && body.content.isNotBlank()) {
            "        setBody(\"${escapeForKotlin(body.content)}\")"
        } else ""

        val innerConfig = listOf(headerLines, bodyLine).filter { it.isNotBlank() }.joinToString("\n")

        return """
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

suspend fun executeRequest() {
    val client = HttpClient(CIO)
    val response: HttpResponse = client.request("${escapeForKotlin(url)}") {
        method = $ktorMethod
$innerConfig
    }
    println("Status: ${'$'}{response.status}")
    println("Body: ${'$'}{response.bodyAsText()}")
    client.close()
}
""".trimIndent()
    }

    // 3. Kotlin (OkHttp)
    private fun generateKotlinOkHttp(
        method: HttpMethod,
        url: String,
        headers: Map<String, String>,
        body: ExtractedBody
    ): String {
        val bodySetup = when {
            body.isFormData -> {
                buildString {
                    appendLine("val requestBody = MultipartBody.Builder()")
                    appendLine("    .setType(MultipartBody.FORM)")
                    body.formFields.forEach { (k, v) ->
                        appendLine("    .addFormDataPart(\"${escapeForKotlin(k)}\", \"${escapeForKotlin(v)}\")")
                    }
                    append("    .build()")
                }
            }
            body.content != null && body.content.isNotBlank() -> {
                val mediaType = body.contentType ?: "application/json; charset=utf-8"
                "val mediaType = \"$mediaType\".toMediaType()\nval requestBody = \"${escapeForKotlin(body.content)}\".toRequestBody(mediaType)"
            }
            method in listOf(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH) -> {
                "val requestBody = \"\".toRequestBody(null)"
            }
            else -> ""
        }

        val methodCall = when (method) {
            HttpMethod.GET -> ".get()"
            HttpMethod.HEAD -> ".head()"
            HttpMethod.DELETE -> if (bodySetup.isNotBlank()) ".delete(requestBody)" else ".delete()"
            HttpMethod.POST -> ".post(requestBody)"
            HttpMethod.PUT -> ".put(requestBody)"
            HttpMethod.PATCH -> ".patch(requestBody)"
            HttpMethod.OPTIONS -> ".method(\"OPTIONS\", null)"
        }

        val headerCalls = headers.map { (k, v) ->
            "        .addHeader(\"${escapeForKotlin(k)}\", \"${escapeForKotlin(v)}\")"
        }.joinToString("\n")

        val bodyBlock = if (bodySetup.isNotBlank()) "    $bodySetup\n" else ""

        return """
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

fun executeRequest() {
    val client = OkHttpClient()
$bodyBlock
    val request = Request.Builder()
        .url("${escapeForKotlin(url)}")
$headerCalls
        $methodCall
        .build()

    client.newCall(request).execute().use { response ->
        println("Status: ${'$'}{response.code}")
        println("Body: ${'$'}{response.body?.string()}")
    }
}
""".trimIndent()
    }

    // 4. Swift (URLSession)
    private fun generateSwiftUrlSession(
        method: HttpMethod,
        url: String,
        headers: Map<String, String>,
        body: ExtractedBody
    ): String {
        val headerLines = headers.map { (k, v) ->
            "request.setValue(\"${escapeForSwift(v)}\", forHTTPHeaderField: \"${escapeForSwift(k)}\")"
        }.joinToString("\n")

        val bodyLines = if (body.content != null && body.content.isNotBlank()) {
            "request.httpBody = \"${escapeForSwift(body.content)}\".data(using: .utf8)"
        } else ""

        return """
import Foundation

func executeRequest() async throws {
    guard let url = URL(string: "${escapeForSwift(url)}") else {
        fatalError("Invalid URL")
    }

    var request = URLRequest(url: url)
    request.httpMethod = "${method.name}"
${if (headerLines.isNotBlank()) "    $headerLines" else ""}
${if (bodyLines.isNotBlank()) "    $bodyLines" else ""}

    let (data, response) = try await URLSession.shared.data(for: request)
    if let httpResponse = response as? HTTPURLResponse {
        print("Status code: \(httpResponse.statusCode)")
    }
    if let responseString = String(data: data, encoding: .utf8) {
        print("Response body:\n\(responseString)")
    }
}
""".trimIndent()
    }

    // 5. Swift (Alamofire)
    private fun generateSwiftAlamofire(
        method: HttpMethod,
        url: String,
        headers: Map<String, String>,
        body: ExtractedBody
    ): String {
        val afMethod = when (method) {
            HttpMethod.GET -> ".get"
            HttpMethod.POST -> ".post"
            HttpMethod.PUT -> ".put"
            HttpMethod.DELETE -> ".delete"
            HttpMethod.PATCH -> ".patch"
            HttpMethod.HEAD -> ".head"
            HttpMethod.OPTIONS -> ".options"
        }

        val headerEntries = headers.map { (k, v) ->
            "        \"${escapeForSwift(k)}\": \"${escapeForSwift(v)}\""
        }.joinToString(",\n")

        return """
import Alamofire
import Foundation

func executeRequest() {
    let url = "${escapeForSwift(url)}"
    let headers: HTTPHeaders = [
$headerEntries
    ]

    AF.request(
        url,
        method: $afMethod,
        headers: headers
    ).responseString { response in
        switch response.result {
        case .success(let value):
            print("Response:\n\(value)")
        case .failure(let error):
            print("Error: \(error)")
        }
    }
}
""".trimIndent()
    }

    // 6. JavaScript (Fetch)
    private fun generateJsFetch(
        method: HttpMethod,
        url: String,
        headers: Map<String, String>,
        body: ExtractedBody
    ): String {
        val headerEntries = headers.map { (k, v) ->
            "    \"${escapeForJs(k)}\": \"${escapeForJs(v)}\""
        }.joinToString(",\n")

        val bodyProp = if (body.content != null && body.content.isNotBlank()) {
            ",\n  body: `${escapeForJsTemplate(body.content)}`"
        } else ""

        return """
async function executeRequest() {
  const url = "${escapeForJs(url)}";
  const options = {
    method: "${method.name}",
    headers: {
$headerEntries
    }$bodyProp
  };

  try {
    const response = await fetch(url, options);
    const result = await response.text();
    console.log("Status:", response.status);
    console.log("Response:", result);
  } catch (error) {
    console.error("Fetch error:", error);
  }
}
""".trimIndent()
    }

    // 7. JavaScript (Axios)
    private fun generateJsAxios(
        method: HttpMethod,
        url: String,
        headers: Map<String, String>,
        body: ExtractedBody
    ): String {
        val headerEntries = headers.map { (k, v) ->
            "    '${escapeForJs(k)}': '${escapeForJs(v)}'"
        }.joinToString(",\n")

        val dataProp = if (body.content != null && body.content.isNotBlank()) {
            ",\n  data: `${escapeForJsTemplate(body.content)}`"
        } else ""

        return """
import axios from 'axios';

async function executeRequest() {
  const config = {
    method: '${method.name.lowercase()}',
    url: '${escapeForJs(url)}',
    headers: {
$headerEntries
    }$dataProp
  };

  try {
    const response = await axios.request(config);
    console.log("Status:", response.status);
    console.log("Data:", response.data);
  } catch (error) {
    console.error("Axios error:", error);
  }
}
""".trimIndent()
    }

    // 8. Python (Requests)
    private fun generatePythonRequests(
        method: HttpMethod,
        url: String,
        headers: Map<String, String>,
        body: ExtractedBody
    ): String {
        val headerEntries = headers.map { (k, v) ->
            "    \"${escapeForPython(k)}\": \"${escapeForPython(v)}\""
        }.joinToString(",\n")

        val dataArg = if (body.content != null && body.content.isNotBlank()) {
            ",\n    data=\"\"\"${body.content}\"\"\""
        } else ""

        return """
import requests

url = "${escapeForPython(url)}"
headers = {
$headerEntries
}

response = requests.request(
    "${method.name}",
    url,
    headers=headers$dataArg
)

print(f"Status Code: {response.status_code}")
print(response.text)
""".trimIndent()
    }

    // 9. Python (HTTPX)
    private fun generatePythonHttpx(
        method: HttpMethod,
        url: String,
        headers: Map<String, String>,
        body: ExtractedBody
    ): String {
        val headerEntries = headers.map { (k, v) ->
            "        \"${escapeForPython(k)}\": \"${escapeForPython(v)}\""
        }.joinToString(",\n")

        val contentArg = if (body.content != null && body.content.isNotBlank()) {
            ",\n        content=\"\"\"${body.content}\"\"\""
        } else ""

        return """
import httpx

url = "${escapeForPython(url)}"
headers = {
$headerEntries
}

with httpx.Client() as client:
    response = client.request(
        "${method.name}",
        url,
        headers=headers$contentArg
    )
    print(f"Status: {response.status_code}")
    print(response.text)
""".trimIndent()
    }

    // 10. Go (net/http)
    private fun generateGoNetHttp(
        method: HttpMethod,
        url: String,
        headers: Map<String, String>,
        body: ExtractedBody
    ): String {
        val (readerInit, readerArg) = if (body.content != null && body.content.isNotBlank()) {
            val payload = "payload := strings.NewReader(`" + body.content.replace("`", "` + \"`\" + `") + "`)\n    "
            Pair(payload, "payload")
        } else {
            Pair("", "nil")
        }

        val headerLines = headers.map { (k, v) ->
            "    req.Header.Add(\"$k\", \"$v\")"
        }.joinToString("\n")

        return """
package main

import (
    "fmt"
    "io"
    "net/http"
    "strings"
)

func main() {
    url := "$url"
    ${readerInit}req, err := http.NewRequest("${method.name}", url, $readerArg)
    if err != nil {
        panic(err)
    }

$headerLines

    res, err := http.DefaultClient.Do(req)
    if err != nil {
        panic(err)
    }
    defer res.Body.Close()

    body, _ := io.ReadAll(res.Body)
    fmt.Printf("Status: %d\n", res.StatusCode)
    fmt.Println(string(body))
}
""".trimIndent()
    }

    // 11. Dart (http)
    private fun generateDartHttp(
        method: HttpMethod,
        url: String,
        headers: Map<String, String>,
        body: ExtractedBody
    ): String {
        val headerEntries = headers.map { (k, v) ->
            "    '$k': '$v'"
        }.joinToString(",\n")

        val dartMethod = method.name.lowercase()
        val bodyParam = if (body.content != null && body.content.isNotBlank()) {
            ",\n    body: '''${body.content}'''"
        } else ""

        return """
import 'package:http/http.dart' as http;

void main() async {
  var url = Uri.parse('$url');
  var headers = {
$headerEntries
  };

  var response = await http.$dartMethod(
    url,
    headers: headers$bodyParam
  );

  print('Response status: ${'$'}{response.statusCode}');
  print('Response body: ${'$'}{response.body}');
}
""".trimIndent()
    }
}
