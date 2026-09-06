package com.learn.reqlite.domain.parser

import com.learn.reqlite.domain.model.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

sealed interface CurlParseResult {
    data class Success(val draft: Draft, val warnings: List<String> = emptyList()) : CurlParseResult
    data class Error(val message: String) : CurlParseResult
}

interface CurlParser {
    fun parse(
        curlCommand: String,
        draftId: String = "draft_imported",
        requestId: String = "req_imported"
    ): CurlParseResult
}

class CurlParserImpl : CurlParser {

    @OptIn(ExperimentalEncodingApi::class)
    override fun parse(
        curlCommand: String,
        draftId: String,
        requestId: String
    ): CurlParseResult {
        val sanitized = sanitize(curlCommand)
        if (sanitized.isBlank()) {
            return CurlParseResult.Error("cURL command cannot be empty")
        }

        val tokens = tokenize(sanitized)
        if (tokens.isEmpty()) {
            return CurlParseResult.Error("No valid tokens found in command")
        }

        val firstToken = tokens.first().lowercase()
        if (firstToken != "curl") {
            return CurlParseResult.Error("Command must start with 'curl'")
        }

        val warnings = mutableListOf<String>()
        var method: HttpMethod? = null
        var rawUrl: String? = null
        val headers = mutableListOf<RequestField>()
        val queryParams = mutableListOf<RequestField>()
        val dataParts = mutableListOf<String>()
        val formParts = mutableListOf<RequestField>()
        var isGetFlag = false
        var isHeadFlag = false
        var basicAuthCredentials: String? = null

        var i = 1
        var fieldCounter = 1

        while (i < tokens.size) {
            val token = tokens[i]

            when {
                // HTTP Method flags
                token == "-X" || token == "--request" -> {
                    if (i + 1 < tokens.size) {
                        val methodStr = tokens[++i]
                        method = parseHttpMethod(methodStr)
                    }
                }
                token.startsWith("-X") && token.length > 2 -> {
                    val methodStr = token.substring(2)
                    method = parseHttpMethod(methodStr)
                }
                token.startsWith("--request=") -> {
                    val methodStr = token.substringAfter("--request=")
                    method = parseHttpMethod(methodStr)
                }

                // Header flags
                token == "-H" || token == "--header" -> {
                    if (i + 1 < tokens.size) {
                        parseHeader(tokens[++i], fieldCounter++)?.let { headers.add(it) }
                    }
                }
                token.startsWith("-H") && token.length > 2 -> {
                    parseHeader(token.substring(2), fieldCounter++)?.let { headers.add(it) }
                }
                token.startsWith("--header=") -> {
                    parseHeader(token.substringAfter("--header="), fieldCounter++)?.let { headers.add(it) }
                }

                // User Agent flag
                token == "-A" || token == "--user-agent" -> {
                    if (i + 1 < tokens.size) {
                        headers.add(RequestField(id = "h_${fieldCounter++}", key = "User-Agent", value = tokens[++i]))
                    }
                }
                token.startsWith("--user-agent=") -> {
                    headers.add(RequestField(id = "h_${fieldCounter++}", key = "User-Agent", value = token.substringAfter("--user-agent=")))
                }

                // Referer flag
                token == "-e" || token == "--referer" -> {
                    if (i + 1 < tokens.size) {
                        headers.add(RequestField(id = "h_${fieldCounter++}", key = "Referer", value = tokens[++i]))
                    }
                }
                token.startsWith("--referer=") -> {
                    headers.add(RequestField(id = "h_${fieldCounter++}", key = "Referer", value = token.substringAfter("--referer=")))
                }

                // Cookie flag
                token == "-b" || token == "--cookie" -> {
                    if (i + 1 < tokens.size) {
                        headers.add(RequestField(id = "h_${fieldCounter++}", key = "Cookie", value = tokens[++i]))
                    }
                }
                token.startsWith("--cookie=") -> {
                    headers.add(RequestField(id = "h_${fieldCounter++}", key = "Cookie", value = token.substringAfter("--cookie=")))
                }

                // Basic Auth flag
                token == "-u" || token == "--user" -> {
                    if (i + 1 < tokens.size) {
                        basicAuthCredentials = tokens[++i]
                    }
                }
                token.startsWith("--user=") -> {
                    basicAuthCredentials = token.substringAfter("--user=")
                }

                // Data / Body flags
                token == "-d" || token == "--data" || token == "--data-raw" || token == "--data-ascii" || token == "--data-binary" -> {
                    if (i + 1 < tokens.size) {
                        dataParts.add(tokens[++i])
                    }
                }
                token.startsWith("-d") && token.length > 2 -> {
                    dataParts.add(token.substring(2))
                }
                token.startsWith("--data=") || token.startsWith("--data-raw=") || token.startsWith("--data-ascii=") || token.startsWith("--data-binary=") -> {
                    dataParts.add(token.substringAfter("="))
                }

                // Form data flag
                token == "-F" || token == "--form" -> {
                    if (i + 1 < tokens.size) {
                        val formStr = tokens[++i]
                        val name = formStr.substringBefore("=").trim()
                        val value = formStr.substringAfter("=", "").trim()
                        formParts.add(RequestField(id = "f_${fieldCounter++}", key = name, value = value))
                    }
                }
                token.startsWith("--form=") -> {
                    val formStr = token.substringAfter("--form=")
                    val name = formStr.substringBefore("=").trim()
                    val value = formStr.substringAfter("=", "").trim()
                    formParts.add(RequestField(id = "f_${fieldCounter++}", key = name, value = value))
                }

                // URL flag
                token == "--url" -> {
                    if (i + 1 < tokens.size) {
                        rawUrl = tokens[++i]
                    }
                }
                token.startsWith("--url=") -> {
                    rawUrl = token.substringAfter("--url=")
                }

                // Method shorthand flags
                token == "-I" || token == "--head" -> {
                    isHeadFlag = true
                }
                token == "-G" || token == "--get" -> {
                    isGetFlag = true
                }

                // Common flags to ignore safely
                token in listOf(
                    "-k", "--insecure", "-L", "--location", "-s", "--silent", "-S", "--show-error",
                    "-v", "--verbose", "-i", "--include", "--compressed", "--no-buffer", "-O", "--remote-name"
                ) -> {
                    // Ignored silently
                }

                // Positional arguments (URL)
                !token.startsWith("-") -> {
                    if (rawUrl == null) {
                        rawUrl = token
                    } else {
                        warnings.add("Ignoring unexpected trailing argument: $token")
                    }
                }

                else -> {
                    warnings.add("Unrecognized or unsupported flag: $token")
                }
            }

            i++
        }

        if (rawUrl == null) {
            return CurlParseResult.Error("No target URL found in cURL command")
        }

        // Basic Auth header injection
        if (basicAuthCredentials != null) {
            val encoded = Base64.encode(basicAuthCredentials.encodeToByteArray())
            val authHeader = RequestField(
                id = "h_${fieldCounter++}",
                key = "Authorization",
                value = "Basic $encoded"
            )
            headers.add(authHeader)
        }

        // Parse URL and Query Parameters
        val (baseUrl, urlParams) = parseUrlAndQueryParams(rawUrl, fieldCounter)
        queryParams.addAll(urlParams)

        // If -G / --get was specified, dataParts are appended to query parameters
        if (isGetFlag && dataParts.isNotEmpty()) {
            dataParts.forEach { part ->
                val pairs = part.split("&")
                pairs.forEach { pair ->
                    if (pair.isNotBlank()) {
                        val key = pair.substringBefore("=").trim()
                        val value = pair.substringAfter("=", "").trim()
                        queryParams.add(RequestField(id = "qp_${fieldCounter++}", key = key, value = value))
                    }
                }
            }
        }

        // Resolve HTTP Method
        val resolvedMethod = when {
            method != null -> method
            isHeadFlag -> HttpMethod.HEAD
            isGetFlag -> HttpMethod.GET
            formParts.isNotEmpty() || dataParts.isNotEmpty() -> HttpMethod.POST
            else -> HttpMethod.GET
        }

        // Resolve Request Body
        val contentTypeHeader = headers.find { it.key.equals("Content-Type", ignoreCase = true) }?.value
        val resolvedBody: RequestBody = when {
            formParts.isNotEmpty() -> RequestBody.FormDataBody(formParts)
            dataParts.isNotEmpty() && !isGetFlag -> {
                val combinedContent = dataParts.joinToString("&")
                val isExplicitUrlEncoded = contentTypeHeader?.contains("application/x-www-form-urlencoded", ignoreCase = true) == true
                val isJson = contentTypeHeader?.contains("application/json", ignoreCase = true) == true ||
                        (contentTypeHeader == null && (combinedContent.trim().startsWith("{") || combinedContent.trim().startsWith("[")))

                when {
                    isExplicitUrlEncoded -> {
                        val fields = combinedContent.split("&").mapNotNull { pair ->
                            if (pair.isNotBlank()) {
                                val k = pair.substringBefore("=")
                                val v = pair.substringAfter("=", "")
                                RequestField(id = "body_field_${fieldCounter++}", key = k, value = v)
                            } else null
                        }
                        RequestBody.UrlEncodedBody(fields)
                    }
                    isJson -> {
                        RequestBody.TextBody(content = combinedContent, contentType = "application/json")
                    }
                    else -> {
                        RequestBody.TextBody(
                            content = combinedContent,
                            contentType = contentTypeHeader ?: "application/x-www-form-urlencoded"
                        )
                    }
                }
            }
            else -> RequestBody.NoBody
        }

        val draft = Draft(
            id = draftId,
            requestId = requestId,
            method = resolvedMethod,
            url = baseUrl,
            headers = headers,
            queryParams = queryParams,
            body = resolvedBody,
            updatedAt = 1000L
        )

        return CurlParseResult.Success(draft, warnings)
    }

    private fun sanitize(input: String): String {
        return input
            .replace("\\\r\n", " ")
            .replace("\\\n", " ")
            .replace("^\r\n", " ")
            .replace("^\n", " ")
            .trim()
    }

    private fun tokenize(input: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var insideSingleQuote = false
        var insideDoubleQuote = false
        var escapeNext = false

        var idx = 0
        while (idx < input.length) {
            val ch = input[idx]

            if (escapeNext) {
                current.append(ch)
                escapeNext = false
                idx++
                continue
            }

            if (ch == '\\' && !insideSingleQuote) {
                escapeNext = true
                idx++
                continue
            }

            if (ch == '\'' && !insideDoubleQuote) {
                insideSingleQuote = !insideSingleQuote
                idx++
                continue
            }

            if (ch == '"' && !insideSingleQuote) {
                insideDoubleQuote = !insideDoubleQuote
                idx++
                continue
            }

            if (ch.isWhitespace() && !insideSingleQuote && !insideDoubleQuote) {
                if (current.isNotEmpty()) {
                    tokens.add(current.toString())
                    current.clear()
                }
                idx++
                continue
            }

            current.append(ch)
            idx++
        }

        if (current.isNotEmpty()) {
            tokens.add(current.toString())
        }

        return tokens
    }

    private fun parseHttpMethod(methodStr: String): HttpMethod {
        return when (methodStr.trim().uppercase()) {
            "POST" -> HttpMethod.POST
            "PUT" -> HttpMethod.PUT
            "DELETE" -> HttpMethod.DELETE
            "PATCH" -> HttpMethod.PATCH
            "HEAD" -> HttpMethod.HEAD
            "OPTIONS" -> HttpMethod.OPTIONS
            else -> HttpMethod.GET
        }
    }

    private fun parseHeader(headerStr: String, idCounter: Int): RequestField? {
        val colonIndex = headerStr.indexOf(':')
        if (colonIndex <= 0) return null

        val key = headerStr.substring(0, colonIndex).trim()
        val value = headerStr.substring(colonIndex + 1).trim()
        return RequestField(id = "h_$idCounter", key = key, value = value)
    }

    private fun parseUrlAndQueryParams(
        urlStr: String,
        startId: Int
    ): Pair<String, List<RequestField>> {
        var idCounter = startId
        val questionMarkIdx = urlStr.indexOf('?')
        if (questionMarkIdx < 0) {
            return Pair(urlStr, emptyList())
        }

        val baseUrl = urlStr.substring(0, questionMarkIdx)
        val queryStr = urlStr.substring(questionMarkIdx + 1)
        val params = mutableListOf<RequestField>()

        val pairs = queryStr.split("&")
        for (pair in pairs) {
            if (pair.isNotBlank()) {
                val key = pair.substringBefore("=").trim()
                val value = pair.substringAfter("=", "").trim()
                params.add(RequestField(id = "qp_${idCounter++}", key = key, value = value))
            }
        }

        return Pair(baseUrl, params)
    }
}
