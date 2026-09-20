package com.learn.reqlite.domain.parser

import com.learn.reqlite.domain.model.Collection
import com.learn.reqlite.domain.model.Folder
import com.learn.reqlite.domain.model.HttpMethod
import com.learn.reqlite.domain.model.Request
import com.learn.reqlite.domain.model.RequestBody
import com.learn.reqlite.domain.model.RequestField
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

sealed interface PostmanParseResult {
    data class Success(
        val collection: Collection,
        val folders: List<Folder>,
        val requests: List<Request>,
        val warnings: List<String> = emptyList()
    ) : PostmanParseResult

    data class Error(val message: String) : PostmanParseResult
}

interface PostmanCollectionParser {
    fun parse(jsonContent: String): PostmanParseResult
}

class PostmanCollectionParserImpl(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
) : PostmanCollectionParser {

    private var idCounter = 1000L

    override fun parse(jsonContent: String): PostmanParseResult {
        if (jsonContent.isBlank()) {
            return PostmanParseResult.Error("Postman collection content cannot be empty")
        }

        val rootElement = try {
            json.parseToJsonElement(jsonContent)
        } catch (e: Exception) {
            return PostmanParseResult.Error("Invalid JSON: ${e.message}")
        }

        val rootObj = rootElement as? JsonObject
            ?: return PostmanParseResult.Error("Root of Postman collection must be a JSON object")

        val infoObj = rootObj["info"] as? JsonObject
            ?: return PostmanParseResult.Error("Missing 'info' object in Postman collection")

        val itemsArray = rootObj["item"] as? JsonArray
            ?: return PostmanParseResult.Error("Missing 'item' array in Postman collection")

        val warnings = mutableListOf<String>()

        val schema = infoObj["schema"].asString() ?: ""
        if (!schema.contains("v2.1") && !schema.contains("v2.0") && !schema.contains("collection.json")) {
            warnings.add("Schema '$schema' may not be Postman v2.0 or v2.1 compatible")
        }

        val collectionName = infoObj["name"].asString()?.trim()?.ifBlank {
            "Imported Postman Collection"
        } ?: "Imported Postman Collection"

        val collectionDesc = extractDescription(infoObj["description"])
        val now = 1000L + idCounter++
        val collectionId = "col_pm_${now}_${(1000..9999).random()}"

        val collection = Collection(
            id = collectionId,
            name = collectionName,
            description = collectionDesc,
            createdAt = now,
            updatedAt = now
        )

        val folders = mutableListOf<Folder>()
        val requests = mutableListOf<Request>()

        parseItems(
            items = itemsArray,
            parentFolderId = null,
            collectionId = collectionId,
            folders = folders,
            requests = requests,
            warnings = warnings
        )

        return PostmanParseResult.Success(
            collection = collection,
            folders = folders,
            requests = requests,
            warnings = warnings
        )
    }

    private fun parseItems(
        items: JsonArray,
        parentFolderId: String?,
        collectionId: String,
        folders: MutableList<Folder>,
        requests: MutableList<Request>,
        warnings: MutableList<String>
    ) {
        for (element in items) {
            val itemObj = element as? JsonObject ?: continue
            val itemName = itemObj["name"].asString()?.trim()?.ifBlank { "Untitled" } ?: "Untitled"
            val itemDesc = extractDescription(itemObj["description"])

            val subItems = itemObj["item"] as? JsonArray
            val requestElement = itemObj["request"]

            if (subItems != null) {
                // Folder
                val now = 1000L + idCounter++
                val folderId = "fld_pm_${now}_${(1000..9999).random()}"
                val folder = Folder(
                    id = folderId,
                    collectionId = collectionId,
                    parentFolderId = parentFolderId,
                    name = itemName,
                    description = itemDesc,
                    createdAt = now,
                    updatedAt = now
                )
                folders.add(folder)

                // Recursively parse nested items
                parseItems(
                    items = subItems,
                    parentFolderId = folderId,
                    collectionId = collectionId,
                    folders = folders,
                    requests = requests,
                    warnings = warnings
                )
            } else if (requestElement != null) {
                // Request
                val request = parseRequest(
                    requestElement = requestElement,
                    name = itemName,
                    itemDescription = itemDesc,
                    collectionId = collectionId,
                    folderId = parentFolderId,
                    warnings = warnings
                )
                if (request != null) {
                    requests.add(request)
                }
            }
        }
    }

    private fun parseRequest(
        requestElement: JsonElement,
        name: String,
        itemDescription: String?,
        collectionId: String,
        folderId: String?,
        warnings: MutableList<String>
    ): Request? {
        val now = 1000L + idCounter++
        val requestId = "req_pm_${now}_${(1000..9999).random()}"

        when (requestElement) {
            is JsonObject -> {
                val methodStr = requestElement["method"].asString()?.trim()?.uppercase() ?: "GET"
                val method = parseHttpMethod(methodStr)

                // URL & Query Params
                val (url, queryParams) = parseUrl(requestElement["url"])

                // Headers
                val headers = parseHeaders(requestElement["header"]).toMutableList()

                // Body
                val body = parseBody(requestElement["body"], headers)

                // Auth
                val authHeaders = parseAuth(requestElement["auth"])
                for (ah in authHeaders) {
                    if (headers.none { it.key.equals(ah.key, ignoreCase = true) }) {
                        headers.add(ah)
                    }
                }

                val reqDesc = extractDescription(requestElement["description"]) ?: itemDescription

                return Request(
                    id = requestId,
                    collectionId = collectionId,
                    folderId = folderId,
                    name = name,
                    method = method,
                    url = url,
                    headers = headers,
                    queryParams = queryParams,
                    body = body,
                    createdAt = now,
                    updatedAt = now
                )
            }
            else -> {
                val urlString = requestElement.asString() ?: ""
                val (url, queryParams) = extractUrlAndQueryParams(urlString)
                return Request(
                    id = requestId,
                    collectionId = collectionId,
                    folderId = folderId,
                    name = name,
                    method = HttpMethod.GET,
                    url = url,
                    headers = emptyList(),
                    queryParams = queryParams,
                    body = RequestBody.NoBody,
                    createdAt = now,
                    updatedAt = now
                )
            }
        }
    }

    private fun parseUrl(urlElement: JsonElement?): Pair<String, List<RequestField>> {
        if (urlElement == null) return Pair("", emptyList())

        when (urlElement) {
            is JsonObject -> {
                val raw = urlElement["raw"].asString()
                val queryArray = urlElement["query"] as? JsonArray

                val queryParams = mutableListOf<RequestField>()
                if (queryArray != null) {
                    for (q in queryArray) {
                        val qObj = q as? JsonObject ?: continue
                        val k = qObj["key"].asString() ?: continue
                        val v = qObj["value"].asString() ?: ""
                        val disabled = qObj["disabled"].asBoolean() ?: false
                        val desc = extractDescription(qObj["description"])
                        val fldId = "fld_q_${1000L + idCounter++}"
                        queryParams.add(
                            RequestField(
                                id = fldId,
                                key = k,
                                value = v,
                                isEnabled = !disabled,
                                description = desc
                            )
                        )
                    }
                }

                val fullUrl = if (!raw.isNullOrBlank()) {
                    raw
                } else {
                    val protocol = urlElement["protocol"].asString() ?: "https"
                    val hostArr = urlElement["host"] as? JsonArray
                    val host = hostArr?.mapNotNull { it.asString() }?.joinToString(".") ?: ""
                    val pathArr = urlElement["path"] as? JsonArray
                    val path = pathArr?.mapNotNull { it.asString() }?.joinToString("/") ?: ""
                    if (host.isNotBlank()) "$protocol://$host/$path" else path
                }

                val baseUrl = if (queryParams.isNotEmpty() && fullUrl.contains("?")) {
                    fullUrl.substringBefore("?")
                } else if (queryParams.isEmpty() && fullUrl.contains("?")) {
                    val (extractedBase, extractedParams) = extractUrlAndQueryParams(fullUrl)
                    return Pair(extractedBase, extractedParams)
                } else {
                    fullUrl
                }

                return Pair(baseUrl, queryParams)
            }
            else -> {
                val urlString = urlElement.asString() ?: ""
                return extractUrlAndQueryParams(urlString)
            }
        }
    }

    private fun extractUrlAndQueryParams(fullUrl: String): Pair<String, List<RequestField>> {
        if (!fullUrl.contains("?")) {
            return Pair(fullUrl, emptyList())
        }

        val baseUrl = fullUrl.substringBefore("?")
        val queryString = fullUrl.substringAfter("?")
        val params = mutableListOf<RequestField>()

        if (queryString.isNotBlank()) {
            val pairs = queryString.split("&")
            for (pair in pairs) {
                if (pair.isBlank()) continue
                val key = pair.substringBefore("=")
                val value = if (pair.contains("=")) pair.substringAfter("=") else ""
                val fldId = "fld_q_${1000L + idCounter++}"
                params.add(
                    RequestField(
                        id = fldId,
                        key = key,
                        value = value,
                        isEnabled = true
                    )
                )
            }
        }

        return Pair(baseUrl, params)
    }

    private fun parseHeaders(headerElement: JsonElement?): List<RequestField> {
        if (headerElement == null) return emptyList()

        val headers = mutableListOf<RequestField>()

        when (headerElement) {
            is JsonArray -> {
                for (h in headerElement) {
                    val hObj = h as? JsonObject ?: continue
                    val k = hObj["key"].asString() ?: continue
                    if (k.isBlank()) continue
                    val v = hObj["value"].asString() ?: ""
                    val disabled = hObj["disabled"].asBoolean() ?: false
                    val desc = extractDescription(hObj["description"])
                    val fldId = "fld_h_${1000L + idCounter++}"
                    headers.add(
                        RequestField(
                            id = fldId,
                            key = k,
                            value = v,
                            isEnabled = !disabled,
                            description = desc
                        )
                    )
                }
            }
            else -> {
                val rawHeaders = headerElement.asString() ?: ""
                for (line in rawHeaders.lines()) {
                    val trimmed = line.trim()
                    if (trimmed.isBlank() || !trimmed.contains(":")) continue
                    val key = trimmed.substringBefore(":").trim()
                    val value = trimmed.substringAfter(":").trim()
                    val fldId = "fld_h_${1000L + idCounter++}"
                    headers.add(
                        RequestField(
                            id = fldId,
                            key = key,
                            value = value,
                            isEnabled = true
                        )
                    )
                }
            }
        }

        return headers
    }

    private fun parseBody(bodyElement: JsonElement?, headers: List<RequestField>): RequestBody {
        val bodyObj = bodyElement as? JsonObject ?: return RequestBody.NoBody

        val mode = bodyObj["mode"].asString()?.lowercase() ?: "none"

        return when (mode) {
            "raw" -> {
                val content = bodyObj["raw"].asString() ?: ""
                val lang = (bodyObj["options"] as? JsonObject)?.get("raw")?.let { (it as? JsonObject)?.get("language").asString() }?.lowercase()
                val contentTypeHeader = headers.firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }?.value
                val contentType = contentTypeHeader ?: when (lang) {
                    "json" -> "application/json"
                    "xml" -> "application/xml"
                    "html" -> "text/html"
                    "javascript" -> "application/javascript"
                    else -> if (content.trim().startsWith("{") || content.trim().startsWith("[")) "application/json" else "text/plain"
                }
                RequestBody.TextBody(content = content, contentType = contentType)
            }
            "urlencoded" -> {
                val urlencodedList = bodyObj["urlencoded"] as? JsonArray ?: return RequestBody.UrlEncodedBody(emptyList())
                val fields = mutableListOf<RequestField>()
                for (item in urlencodedList) {
                    val itemObj = item as? JsonObject ?: continue
                    val k = itemObj["key"].asString() ?: continue
                    val v = itemObj["value"].asString() ?: ""
                    val disabled = itemObj["disabled"].asBoolean() ?: false
                    val desc = extractDescription(itemObj["description"])
                    val fldId = "fld_b_${1000L + idCounter++}"
                    fields.add(
                        RequestField(
                            id = fldId,
                            key = k,
                            value = v,
                            isEnabled = !disabled,
                            description = desc
                        )
                    )
                }
                RequestBody.UrlEncodedBody(fields = fields)
            }
            "formdata" -> {
                val formdataList = bodyObj["formdata"] as? JsonArray ?: return RequestBody.FormDataBody(emptyList())
                val parts = mutableListOf<RequestField>()
                for (item in formdataList) {
                    val itemObj = item as? JsonObject ?: continue
                    val k = itemObj["key"].asString() ?: continue
                    val v = itemObj["value"].asString() ?: ""
                    val disabled = itemObj["disabled"].asBoolean() ?: false
                    val desc = extractDescription(itemObj["description"])
                    val fldId = "fld_f_${1000L + idCounter++}"
                    parts.add(
                        RequestField(
                            id = fldId,
                            key = k,
                            value = v,
                            isEnabled = !disabled,
                            description = desc
                        )
                    )
                }
                RequestBody.FormDataBody(parts = parts)
            }
            "graphql" -> {
                val gqlObj = bodyObj["graphql"] as? JsonObject
                val query = gqlObj?.get("query").asString() ?: ""
                val variables = gqlObj?.get("variables").asString()
                val jsonPayload = if (variables.isNullOrBlank()) {
                    "{\"query\": \"${query.replace("\"", "\\\"").replace("\n", "\\n")}\"}"
                } else {
                    "{\"query\": \"${query.replace("\"", "\\\"").replace("\n", "\\n")}\", \"variables\": $variables}"
                }
                RequestBody.TextBody(content = jsonPayload, contentType = "application/json")
            }
            else -> RequestBody.NoBody
        }
    }

    private fun parseAuth(authElement: JsonElement?): List<RequestField> {
        val authObj = authElement as? JsonObject ?: return emptyList()
        val type = authObj["type"].asString()?.lowercase() ?: return emptyList()

        val fields = mutableListOf<RequestField>()

        when (type) {
            "bearer" -> {
                val bearerArr = authObj["bearer"] as? JsonArray
                val token = bearerArr?.firstOrNull {
                    (it as? JsonObject)?.get("key").asString() == "token"
                }?.let { (it as? JsonObject)?.get("value").asString() }

                if (!token.isNullOrBlank()) {
                    fields.add(
                        RequestField(
                            id = "fld_auth_${1000L + idCounter++}",
                            key = "Authorization",
                            value = "Bearer $token",
                            isEnabled = true
                        )
                    )
                }
            }
            "apikey" -> {
                val apikeyArr = authObj["apikey"] as? JsonArray
                val key = apikeyArr?.firstOrNull { (it as? JsonObject)?.get("key").asString() == "key" }?.let { (it as? JsonObject)?.get("value").asString() }
                val value = apikeyArr?.firstOrNull { (it as? JsonObject)?.get("key").asString() == "value" }?.let { (it as? JsonObject)?.get("value").asString() }
                val inLocation = apikeyArr?.firstOrNull { (it as? JsonObject)?.get("key").asString() == "in" }?.let { (it as? JsonObject)?.get("value").asString() } ?: "header"

                if (!key.isNullOrBlank() && !value.isNullOrBlank() && inLocation.lowercase() == "header") {
                    fields.add(
                        RequestField(
                            id = "fld_auth_${1000L + idCounter++}",
                            key = key,
                            value = value,
                            isEnabled = true
                        )
                    )
                }
            }
        }

        return fields
    }

    private fun extractDescription(descElement: JsonElement?): String? {
        if (descElement == null) return null
        return when (descElement) {
            is JsonObject -> descElement["content"].asString()
            else -> descElement.asString()
        }
    }

    private fun parseHttpMethod(method: String): HttpMethod {
        return try {
            HttpMethod.valueOf(method.uppercase())
        } catch (_: Exception) {
            HttpMethod.GET
        }
    }

    private fun JsonElement?.asString(): String? {
        val prim = this as? JsonPrimitive ?: return null
        return if (prim.isString || prim.content != "null") prim.content else null
    }

    private fun JsonElement?.asBoolean(): Boolean? {
        return (this as? JsonPrimitive)?.booleanOrNull
    }
}
