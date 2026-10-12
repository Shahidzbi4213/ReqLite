package com.learn.reqlite.domain.parser

import com.learn.reqlite.domain.model.Collection
import com.learn.reqlite.domain.model.Environment
import com.learn.reqlite.domain.model.Folder
import com.learn.reqlite.domain.model.HttpMethod
import com.learn.reqlite.domain.model.Request
import com.learn.reqlite.domain.model.RequestBody
import com.learn.reqlite.domain.model.RequestField
import com.learn.reqlite.domain.model.Variable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed interface InsomniaParseResult {
    data class Success(
        val collection: Collection,
        val folders: List<Folder>,
        val requests: List<Request>,
        val environments: List<Environment> = emptyList(),
        val warnings: List<String> = emptyList()
    ) : InsomniaParseResult

    data class Error(val message: String) : InsomniaParseResult
}

interface InsomniaCollectionParser {
    fun parse(jsonContent: String): InsomniaParseResult
}

class InsomniaCollectionParserImpl(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
) : InsomniaCollectionParser {

    private var idCounter = 2000L

    override fun parse(jsonContent: String): InsomniaParseResult {
        if (jsonContent.isBlank()) {
            return InsomniaParseResult.Error("Insomnia collection content cannot be empty")
        }

        val rootElement = try {
            json.parseToJsonElement(jsonContent)
        } catch (e: Exception) {
            return InsomniaParseResult.Error("Invalid JSON: ${e.message}")
        }

        val rootObj = rootElement as? JsonObject
            ?: return InsomniaParseResult.Error("Root of Insomnia collection must be a JSON object")

        val resourcesArray = rootObj["resources"] as? JsonArray
            ?: return InsomniaParseResult.Error("Missing 'resources' array in Insomnia export")

        val warnings = mutableListOf<String>()
        val exportFormat = rootObj["__export_format"]?.asInt()
        if (exportFormat != null && exportFormat < 4) {
            warnings.add("Export format v$exportFormat may have limited feature support (v4+ recommended)")
        }

        val now = 2000L + idCounter++

        // Categorize resources by _type
        val workspaceResources = mutableListOf<JsonObject>()
        val folderResources = mutableListOf<JsonObject>()
        val requestResources = mutableListOf<JsonObject>()
        val environmentResources = mutableListOf<JsonObject>()

        for (resource in resourcesArray) {
            val obj = resource as? JsonObject ?: continue
            when (obj["_type"].asString()) {
                "workspace" -> workspaceResources.add(obj)
                "request_group" -> folderResources.add(obj)
                "request" -> requestResources.add(obj)
                "environment" -> environmentResources.add(obj)
            }
        }

        // 1. Resolve Collection from workspace
        val primaryWorkspace = workspaceResources.firstOrNull()
        val collectionId = primaryWorkspace?.get("_id").asString()?.ifBlank { null }
            ?: "col_ins_${now}_${(1000..9999).random()}"
        val collectionName = primaryWorkspace?.get("name").asString()?.trim()?.ifBlank { null }
            ?: "Imported Insomnia Collection"
        val collectionDesc = primaryWorkspace?.get("description").asString()

        val collection = Collection(
            id = collectionId,
            name = collectionName,
            description = collectionDesc,
            createdAt = primaryWorkspace?.get("created")?.asLong() ?: now,
            updatedAt = primaryWorkspace?.get("modified")?.asLong() ?: now
        )

        // Set of valid folder IDs for parent tracking
        val folderIdSet = folderResources.mapNotNull { it["_id"].asString() }.toSet()

        // 2. Resolve Folders (request_group)
        val folders = folderResources.map { fObj ->
            val fId = fObj["_id"].asString() ?: "fld_${now}_${(1000..9999).random()}"
            val parentId = fObj["parentId"].asString()
            // If parentId is another folder, link it; otherwise null (top-level under workspace)
            val parentFolderId = if (parentId != null && folderIdSet.contains(parentId)) parentId else null

            Folder(
                id = fId,
                collectionId = collectionId,
                parentFolderId = parentFolderId,
                name = fObj["name"].asString()?.trim()?.ifBlank { "Folder" } ?: "Folder",
                description = fObj["description"].asString(),
                createdAt = fObj["created"]?.asLong() ?: now,
                updatedAt = fObj["modified"]?.asLong() ?: now
            )
        }

        // 3. Resolve Environments
        val environments = environmentResources.mapNotNull { envObj ->
            val envId = envObj["_id"].asString() ?: "env_${now}_${(1000..9999).random()}"
            val envName = envObj["name"].asString()?.trim()?.ifBlank { "Environment" } ?: "Environment"
            val envColor = envObj["color"].asString()
            val dataObj = envObj["data"] as? JsonObject ?: JsonObject(emptyMap())

            val variables = dataObj.entries.map { (key, valueElement) ->
                val strValue = when (valueElement) {
                    is JsonPrimitive -> valueElement.content
                    else -> valueElement.toString()
                }
                Variable(
                    id = "var_${key}_${now}_${(100..999).random()}",
                    key = key,
                    value = strValue,
                    isEnabled = true,
                    isSecret = envObj["isPrivate"]?.asBoolean() ?: false
                )
            }

            Environment(
                id = envId,
                name = envName,
                color = envColor,
                variables = variables,
                createdAt = envObj["created"]?.asLong() ?: now,
                updatedAt = envObj["modified"]?.asLong() ?: now
            )
        }

        // 4. Resolve Requests
        val requests = requestResources.mapNotNull { reqObj ->
            val rId = reqObj["_id"].asString() ?: "req_${now}_${(1000..9999).random()}"
            val parentId = reqObj["parentId"].asString()
            val folderId = if (parentId != null && folderIdSet.contains(parentId)) parentId else null

            val rawUrl = reqObj["url"].asString() ?: ""
            val normalizedUrl = normalizeVariables(rawUrl)

            val rawMethod = reqObj["method"].asString() ?: "GET"
            val httpMethod = parseHttpMethod(rawMethod)

            val name = reqObj["name"].asString()?.trim()?.ifBlank { null }
                ?: normalizedUrl.ifBlank { "Untitled Request" }

            // Headers
            val headersList = mutableListOf<RequestField>()
            val headersArray = reqObj["headers"] as? JsonArray
            if (headersArray != null) {
                for (hElement in headersArray) {
                    val hObj = hElement as? JsonObject ?: continue
                    val hName = hObj["name"].asString() ?: continue
                    val hVal = hObj["value"].asString() ?: ""
                    val disabled = hObj["disabled"]?.asBoolean() ?: false
                    headersList.add(
                        RequestField(
                            id = "fld_${now}_${(1000..9999).random()}",
                            key = normalizeVariables(hName),
                            value = normalizeVariables(hVal),
                            isEnabled = !disabled,
                            description = hObj["description"].asString()
                        )
                    )
                }
            }

            // Authentication injection
            val authObj = reqObj["authentication"] as? JsonObject
            if (authObj != null) {
                injectAuthentication(authObj, headersList, now)
            }

            // Query parameters
            val queryParamsList = mutableListOf<RequestField>()
            val paramsArray = reqObj["parameters"] as? JsonArray
            if (paramsArray != null) {
                for (pElement in paramsArray) {
                    val pObj = pElement as? JsonObject ?: continue
                    val pName = pObj["name"].asString() ?: continue
                    val pVal = pObj["value"].asString() ?: ""
                    val disabled = pObj["disabled"]?.asBoolean() ?: false
                    queryParamsList.add(
                        RequestField(
                            id = "fld_${now}_${(1000..9999).random()}",
                            key = normalizeVariables(pName),
                            value = normalizeVariables(pVal),
                            isEnabled = !disabled,
                            description = pObj["description"].asString()
                        )
                    )
                }
            }

            // Request Body
            val body = parseRequestBody(reqObj["body"] as? JsonObject, now)

            Request(
                id = rId,
                collectionId = collectionId,
                folderId = folderId,
                name = name,
                method = httpMethod,
                url = normalizedUrl,
                headers = headersList,
                queryParams = queryParamsList,
                body = body,
                createdAt = reqObj["created"]?.asLong() ?: now,
                updatedAt = reqObj["modified"]?.asLong() ?: now
            )
        }

        return InsomniaParseResult.Success(
            collection = collection,
            folders = folders,
            requests = requests,
            environments = environments,
            warnings = warnings
        )
    }

    private fun parseRequestBody(bodyObj: JsonObject?, now: Long): RequestBody {
        if (bodyObj == null) return RequestBody.NoBody

        val mimeType = bodyObj["mimeType"].asString()?.lowercase()?.trim() ?: ""
        val text = bodyObj["text"].asString() ?: ""
        val paramsArray = bodyObj["params"] as? JsonArray

        return when {
            mimeType.contains("graphql") -> {
                // Insomnia stores GraphQL body either as JSON or as raw query text
                var query = text
                var variables = "{}"
                var operationName: String? = null

                if (text.trim().startsWith("{")) {
                    try {
                        val parsed = json.parseToJsonElement(text).jsonObject
                        val q = parsed["query"]?.jsonPrimitive?.contentOrNull
                        if (q != null) {
                            query = q
                            variables = parsed["variables"]?.toString() ?: "{}"
                            operationName = parsed["operationName"]?.jsonPrimitive?.contentOrNull
                        }
                    } catch (_: Exception) {
                        // Keep text as query
                    }
                }
                RequestBody.GraphQLBody(
                    query = normalizeVariables(query),
                    variables = normalizeVariables(variables),
                    operationName = operationName
                )
            }

            mimeType.contains("x-www-form-urlencoded") -> {
                val fields = paramsArray?.mapNotNull { pElem ->
                    val pObj = pElem as? JsonObject ?: return@mapNotNull null
                    val name = pObj["name"].asString() ?: return@mapNotNull null
                    val value = pObj["value"].asString() ?: ""
                    val disabled = pObj["disabled"]?.asBoolean() ?: false
                    RequestField(
                        id = "fld_${now}_${(1000..9999).random()}",
                        key = normalizeVariables(name),
                        value = normalizeVariables(value),
                        isEnabled = !disabled,
                        description = pObj["description"].asString()
                    )
                } ?: emptyList()
                RequestBody.UrlEncodedBody(fields)
            }

            mimeType.contains("form-data") || mimeType.contains("multipart") -> {
                val parts = paramsArray?.mapNotNull { pElem ->
                    val pObj = pElem as? JsonObject ?: return@mapNotNull null
                    val name = pObj["name"].asString() ?: return@mapNotNull null
                    val value = pObj["value"].asString() ?: ""
                    val disabled = pObj["disabled"]?.asBoolean() ?: false
                    RequestField(
                        id = "fld_${now}_${(1000..9999).random()}",
                        key = normalizeVariables(name),
                        value = normalizeVariables(value),
                        isEnabled = !disabled,
                        description = pObj["description"].asString()
                    )
                } ?: emptyList()
                RequestBody.FormDataBody(parts)
            }

            !text.isBlank() -> {
                val contentType = if (mimeType.isNotBlank()) mimeType else "text/plain"
                RequestBody.TextBody(
                    content = normalizeVariables(text),
                    contentType = contentType
                )
            }

            else -> RequestBody.NoBody
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun injectAuthentication(
        authObj: JsonObject,
        headersList: MutableList<RequestField>,
        now: Long
    ) {
        val disabled = authObj["disabled"]?.asBoolean() ?: false
        if (disabled) return

        val type = authObj["type"].asString()?.lowercase() ?: return
        when (type) {
            "bearer" -> {
                val token = authObj["token"].asString()
                val prefix = authObj["prefix"].asString()?.ifBlank { "Bearer" } ?: "Bearer"
                if (!token.isNullOrBlank()) {
                    if (headersList.none { it.key.equals("Authorization", ignoreCase = true) }) {
                        headersList.add(
                            RequestField(
                                id = "fld_auth_${now}_${(1000..9999).random()}",
                                key = "Authorization",
                                value = "$prefix ${normalizeVariables(token)}",
                                isEnabled = true
                            )
                        )
                    }
                }
            }

            "basic" -> {
                val username = authObj["username"].asString() ?: ""
                val password = authObj["password"].asString() ?: ""
                if (username.isNotEmpty() || password.isNotEmpty()) {
                    val raw = "$username:$password"
                    val encoded = Base64.Default.encode(raw.encodeToByteArray())
                    if (headersList.none { it.key.equals("Authorization", ignoreCase = true) }) {
                        headersList.add(
                            RequestField(
                                id = "fld_auth_${now}_${(1000..9999).random()}",
                                key = "Authorization",
                                value = "Basic $encoded",
                                isEnabled = true
                            )
                        )
                    }
                }
            }

            "apikey" -> {
                val key = authObj["key"].asString()
                val value = authObj["value"].asString()
                val addTo = authObj["addTo"].asString()?.lowercase() ?: "header"
                if (!key.isNullOrBlank() && !value.isNullOrBlank() && addTo == "header") {
                    if (headersList.none { it.key.equals(key, ignoreCase = true) }) {
                        headersList.add(
                            RequestField(
                                id = "fld_auth_${now}_${(1000..9999).random()}",
                                key = normalizeVariables(key),
                                value = normalizeVariables(value),
                                isEnabled = true
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Normalizes Insomnia variable syntax:
     * e.g., `{{ _.baseUrl }}` or `{{ baseUrl }}` or `{{base_url}}` into standard `{{baseUrl}}`.
     */
    private fun normalizeVariables(input: String): String {
        if (!input.contains("{{")) return input
        // Replace {{ _.var }} or {{ _.var}} or {{_.var}} -> {{var}}
        var result = input.replace(Regex("""\{\{\s*_\.([a-zA-Z0-9_\-\.]+)\s*\}\}""")) { match ->
            "{{${match.groupValues[1]}}}"
        }
        // Replace {{ var }} -> {{var}}
        result = result.replace(Regex("""\{\{\s+([a-zA-Z0-9_\-\.]+)\s+\}\}""")) { match ->
            "{{${match.groupValues[1]}}}"
        }
        return result
    }

    private fun parseHttpMethod(method: String): HttpMethod {
        return when (method.trim().uppercase()) {
            "POST" -> HttpMethod.POST
            "PUT" -> HttpMethod.PUT
            "DELETE" -> HttpMethod.DELETE
            "PATCH" -> HttpMethod.PATCH
            "HEAD" -> HttpMethod.HEAD
            "OPTIONS" -> HttpMethod.OPTIONS
            else -> HttpMethod.GET
        }
    }

    private fun JsonElement?.asString(): String? {
        val primitive = this as? JsonPrimitive ?: return null
        return primitive.contentOrNull
    }

    private fun JsonElement?.asBoolean(): Boolean? {
        val primitive = this as? JsonPrimitive ?: return null
        return primitive.booleanOrNull
    }

    private fun JsonElement?.asInt(): Int? {
        val primitive = this as? JsonPrimitive ?: return null
        return primitive.contentOrNull?.toIntOrNull()
    }

    private fun JsonElement?.asLong(): Long? {
        val primitive = this as? JsonPrimitive ?: return null
        return primitive.contentOrNull?.toLongOrNull()
    }
}
