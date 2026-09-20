package com.learn.reqlite.domain.parser

import com.learn.reqlite.domain.model.*
import kotlinx.serialization.json.*

sealed interface SmartPayload {
    data class RequestConfig(
        val url: String,
        val method: HttpMethod = HttpMethod.GET,
        val headers: List<RequestField> = emptyList(),
        val queryParams: List<RequestField> = emptyList(),
        val body: RequestBody = RequestBody.NoBody,
        val bearerToken: String? = null,
        val description: String? = null
    ) : SmartPayload

    data class AuthToken(
        val token: String,
        val type: String = "Bearer"
    ) : SmartPayload

    data class PlainUrl(
        val url: String
    ) : SmartPayload

    data class Error(
        val message: String
    ) : SmartPayload
}

class SmartPayloadParser(
    private val curlParser: CurlParser = CurlParserImpl(),
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true }
) {
    fun parse(rawInput: String): SmartPayload {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return SmartPayload.Error("Input is empty")
        }

        // 1. Check if it's a cURL command
        if (trimmed.startsWith("curl", ignoreCase = true)) {
            val curlResult = curlParser.parse(trimmed)
            if (curlResult is CurlParseResult.Success) {
                val draft = curlResult.draft
                var bearerToken: String? = null
                val filteredHeaders = draft.headers.filter { header ->
                    if (header.key.equals("Authorization", ignoreCase = true) &&
                        header.value.startsWith("Bearer ", ignoreCase = true)
                    ) {
                        bearerToken = header.value.substring(7).trim()
                        false
                    } else {
                        true
                    }
                }
                return SmartPayload.RequestConfig(
                    url = draft.url,
                    method = draft.method,
                    headers = filteredHeaders,
                    queryParams = draft.queryParams,
                    body = draft.body,
                    bearerToken = bearerToken,
                    description = "Imported from cURL"
                )
            }
        }

        // 2. Check if it's a JSON configuration or JSON Auth Token
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                val element = json.parseToJsonElement(trimmed)
                if (element is JsonObject) {
                    val tokenElem = element["token"] ?: element["authToken"] ?: element["access_token"] ?: element["bearerToken"]
                    val urlElem = element["url"] ?: element["endpoint"]

                    if (urlElem != null && urlElem is JsonPrimitive && urlElem.isString) {
                        val url = urlElem.content
                        val methodStr = (element["method"] as? JsonPrimitive)?.content?.uppercase() ?: "GET"
                        val method = try {
                            HttpMethod.valueOf(methodStr)
                        } catch (e: Exception) {
                            HttpMethod.GET
                        }

                        var bearerToken: String? = (tokenElem as? JsonPrimitive)?.content
                        val headersList = mutableListOf<RequestField>()
                        var counter = 1

                        val headersObj = element["headers"]
                        if (headersObj is JsonObject) {
                            for ((k, v) in headersObj) {
                                val valueStr = (v as? JsonPrimitive)?.content ?: v.toString()
                                if (k.equals("Authorization", ignoreCase = true) && valueStr.startsWith("Bearer ", ignoreCase = true)) {
                                    bearerToken = valueStr.substring(7).trim()
                                } else {
                                    headersList.add(RequestField("h_${counter++}", k, valueStr))
                                }
                            }
                        } else if (headersObj is JsonArray) {
                            for (item in headersObj) {
                                if (item is JsonObject) {
                                    val k = (item["key"] as? JsonPrimitive)?.content ?: ""
                                    val v = (item["value"] as? JsonPrimitive)?.content ?: ""
                                    if (k.isNotBlank()) {
                                        if (k.equals("Authorization", ignoreCase = true) && v.startsWith("Bearer ", ignoreCase = true)) {
                                            bearerToken = v.substring(7).trim()
                                        } else {
                                            headersList.add(RequestField("h_${counter++}", k, v))
                                        }
                                    }
                                }
                            }
                        }

                        val bodyContent = (element["body"] as? JsonPrimitive)?.content
                            ?: (element["body"] as? JsonObject)?.toString()
                        val reqBody = if (!bodyContent.isNullOrBlank()) {
                            RequestBody.TextBody(bodyContent, "application/json")
                        } else {
                            RequestBody.NoBody
                        }

                        return SmartPayload.RequestConfig(
                            url = url,
                            method = method,
                            headers = headersList,
                            queryParams = emptyList(),
                            body = reqBody,
                            bearerToken = bearerToken,
                            description = "Imported from JSON Config"
                        )
                    } else if (tokenElem != null && tokenElem is JsonPrimitive && tokenElem.isString) {
                        return SmartPayload.AuthToken(
                            token = tokenElem.content,
                            type = (element["type"] as? JsonPrimitive)?.content ?: "Bearer"
                        )
                    }
                }
            } catch (_: Exception) {
                // Not valid JSON, fall through
            }
        }

        // 3. Check for Direct Auth Token
        if (trimmed.startsWith("Bearer ", ignoreCase = true)) {
            val token = trimmed.substring(7).trim()
            if (token.isNotEmpty()) {
                return SmartPayload.AuthToken(token = token, type = "Bearer")
            }
        }

        // JWT token heuristic: starts with eyJ and contains two dots
        if (trimmed.startsWith("eyJ") && trimmed.count { it == '.' } == 2) {
            return SmartPayload.AuthToken(token = trimmed, type = "Bearer")
        }

        // 4. Fallback to Plain URL
        return SmartPayload.PlainUrl(url = trimmed)
    }
}
