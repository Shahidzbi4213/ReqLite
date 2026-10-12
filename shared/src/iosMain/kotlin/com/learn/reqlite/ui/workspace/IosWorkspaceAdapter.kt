package com.learn.reqlite.ui.workspace

import com.learn.reqlite.di.DiHelper
import com.learn.reqlite.domain.engine.RequestExecutionEngine
import com.learn.reqlite.domain.model.*
import com.learn.reqlite.domain.repository.CollectionRepository
import com.learn.reqlite.domain.repository.EnvironmentRepository
import com.learn.reqlite.domain.repository.HistoryRepository
import com.learn.reqlite.domain.repository.RequestRepository
import com.learn.reqlite.domain.export.WorkspaceExportImportManager
import com.learn.reqlite.domain.export.WorkspaceImporter
import com.learn.reqlite.domain.export.WorkspaceExporter
import com.learn.reqlite.domain.export.WorkspaceImportResult
import com.learn.reqlite.domain.parser.PostmanCollectionParser
import com.learn.reqlite.domain.parser.PostmanParseResult
import com.learn.reqlite.domain.parser.InsomniaCollectionParser
import com.learn.reqlite.domain.parser.InsomniaCollectionParserImpl
import com.learn.reqlite.domain.parser.InsomniaParseResult
import com.learn.reqlite.domain.parser.SmartPayload
import com.learn.reqlite.domain.parser.SmartPayloadParser
import com.learn.reqlite.domain.graphql.autocomplete.GraphQLAutocompleteEngine
import com.learn.reqlite.domain.graphql.autocomplete.GraphQLCompletionItem
import com.learn.reqlite.domain.graphql.explorer.GraphQLSchemaExplorer
import com.learn.reqlite.domain.graphql.explorer.SchemaExplorerCategory
import com.learn.reqlite.domain.graphql.explorer.SchemaExplorerItem
import com.learn.reqlite.domain.graphql.explorer.SchemaItemKind
import com.learn.reqlite.domain.graphql.format.GraphQLFormatter
import com.learn.reqlite.domain.graphql.model.GraphQLSchema
import com.learn.reqlite.domain.graphql.parser.GraphQLIntrospectionParser
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import platform.posix.time

data class IosSmartPayloadResult(
    val type: String, // "config", "auth", "url", "error"
    val url: String = "",
    val method: String = "GET",
    val headers: List<RequestField> = emptyList(),
    val queryParams: List<RequestField> = emptyList(),
    val bodyContent: String? = null,
    val bearerToken: String? = null,
    val errorMessage: String? = null
)

sealed class IosExecutionState {
    object Idle : IosExecutionState()
    object Loading : IosExecutionState()
    data class Success(
        val result: HistoryEntry,
        val responseBody: String,
        val responseHeaders: Map<String, String> = emptyMap()
    ) : IosExecutionState()
    data class Error(val message: String) : IosExecutionState()
}

class IosWorkspaceAdapter(
    private val executionEngine: RequestExecutionEngine,
    private val environmentRepository: EnvironmentRepository,
    private val requestRepository: RequestRepository,
    private val historyRepository: HistoryRepository,
    private val collectionRepository: CollectionRepository,
    workspaceImporter: WorkspaceImporter? = null,
    workspaceExporter: WorkspaceExporter? = null,
    postmanParser: PostmanCollectionParser? = null,
    insomniaParser: InsomniaCollectionParser? = null,
    private val scope: CoroutineScope
) {
    private val postmanParser: PostmanCollectionParser = postmanParser ?: com.learn.reqlite.domain.parser.PostmanCollectionParserImpl()
    private val insomniaParser: InsomniaCollectionParser = insomniaParser ?: InsomniaCollectionParserImpl()
    private val exportImportManager: WorkspaceExportImportManager by lazy {
        WorkspaceExportImportManager(
            collectionRepository = collectionRepository,
            requestRepository = requestRepository,
            environmentRepository = environmentRepository,
            postmanParser = this.postmanParser,
            insomniaParser = this.insomniaParser
        )
    }
    private val workspaceImporter: WorkspaceImporter = workspaceImporter ?: exportImportManager
    private val workspaceExporter: WorkspaceExporter = workspaceExporter ?: exportImportManager

    constructor() : this(
        executionEngine = DiHelper.getRequestExecutionEngine(),
        environmentRepository = DiHelper.getEnvironmentRepository(),
        requestRepository = DiHelper.getRequestRepository(),
        historyRepository = DiHelper.getHistoryRepository(),
        collectionRepository = DiHelper.getCollectionRepository(),
        workspaceImporter = DiHelper.getWorkspaceImporter(),
        workspaceExporter = DiHelper.getWorkspaceExporter(),
        postmanParser = DiHelper.getPostmanCollectionParser(),
        insomniaParser = DiHelper.getInsomniaCollectionParser(),
        scope = CoroutineScope(Dispatchers.Main)
    )

    private var envJob: Job? = null
    private var historyJob: Job? = null
    private var collectionsJob: Job? = null
    private var requestsJob: Job? = null
    private var executionJob: Job? = null

    fun observeEnvironments(onEnvironmentsChanged: (List<Environment>) -> Unit) {
        envJob?.cancel()
        envJob = environmentRepository.getAllEnvironments()
            .onEach { onEnvironmentsChanged(it) }
            .launchIn(scope)
    }

    fun observeHistory(onHistoryChanged: (List<HistoryEntry>) -> Unit) {
        historyJob?.cancel()
        historyJob = historyRepository.getAllHistoryEntries()
            .onEach { onHistoryChanged(it) }
            .launchIn(scope)
    }

    fun deleteHistoryEntry(id: String) {
        scope.launch {
            historyRepository.deleteHistoryEntry(id)
        }
    }

    fun clearHistory() {
        scope.launch {
            historyRepository.clearHistory()
        }
    }

    fun executeRequest(
        draftId: String,
        environmentId: String?,
        onStateChanged: (IosExecutionState) -> Unit
    ) {
        executionJob?.cancel()
        executionJob = scope.launch {
            onStateChanged(IosExecutionState.Loading)
            try {
                val result = executionEngine.executeWithHeaders(draftId, environmentId)
                onStateChanged(IosExecutionState.Success(result.historyEntry, result.responseBody, result.responseHeaders))
            } catch (e: Exception) {
                onStateChanged(IosExecutionState.Error(e.message ?: "Execution failed"))
            }
        }
    }

    fun parseSmartPayload(rawInput: String): IosSmartPayloadResult {
        val parser = SmartPayloadParser()
        return when (val parsed = parser.parse(rawInput)) {
            is SmartPayload.RequestConfig -> {
                val bodyText = when (val b = parsed.body) {
                    is RequestBody.TextBody -> b.content
                    is RequestBody.UrlEncodedBody -> b.fields.filter { it.isEnabled }.joinToString("&") { "${it.key}=${it.value}" }
                    else -> null
                }
                IosSmartPayloadResult(
                    type = "config",
                    url = parsed.url,
                    method = parsed.method.name,
                    headers = parsed.headers,
                    queryParams = parsed.queryParams,
                    bodyContent = bodyText,
                    bearerToken = parsed.bearerToken
                )
            }
            is SmartPayload.AuthToken -> {
                IosSmartPayloadResult(
                    type = "auth",
                    bearerToken = parsed.token
                )
            }
            is SmartPayload.PlainUrl -> {
                IosSmartPayloadResult(
                    type = "url",
                    url = parsed.url
                )
            }
            is SmartPayload.Error -> {
                IosSmartPayloadResult(
                    type = "error",
                    errorMessage = parsed.message
                )
            }
        }
    }

    fun parseCurl(curlCommand: String): IosSmartPayloadResult = parseSmartPayload(curlCommand)

    fun cancelExecution(onStateChanged: (IosExecutionState) -> Unit) {
        executionJob?.cancel()
        onStateChanged(IosExecutionState.Idle)
    }

    fun createQuickDraft(
        methodName: String,
        url: String,
        onCreated: (String) -> Unit
    ) {
        createDraftWithDetails(
            methodName = methodName,
            url = url,
            headers = emptyList(),
            queryParams = emptyList(),
            bodyContent = null,
            onCreated = onCreated
        )
    }

    fun createDraftWithDetails(
        methodName: String,
        url: String,
        headers: List<RequestField> = emptyList(),
        queryParams: List<RequestField> = emptyList(),
        bodyContent: String? = null,
        isGraphQL: Boolean = false,
        variablesContent: String? = null,
        onCreated: (String) -> Unit
    ) {
        scope.launch {
            val method = when (methodName.uppercase()) {
                "POST" -> HttpMethod.POST
                "PUT" -> HttpMethod.PUT
                "DELETE" -> HttpMethod.DELETE
                "PATCH" -> HttpMethod.PATCH
                "HEAD" -> HttpMethod.HEAD
                "OPTIONS" -> HttpMethod.OPTIONS
                else -> HttpMethod.GET
            }
            val requestBody = when {
                isGraphQL -> RequestBody.GraphQLBody(bodyContent ?: "", variablesContent ?: "{}")
                !bodyContent.isNullOrBlank() -> RequestBody.TextBody(bodyContent, "application/json")
                else -> RequestBody.NoBody
            }
            val draft = Draft(
                id = "ios_draft_${com.learn.reqlite.utils.common.nowMs()}_${(1000..9999).random()}",
                requestId = null,
                method = method,
                url = url,
                headers = headers,
                queryParams = queryParams,
                body = requestBody,
                updatedAt = com.learn.reqlite.utils.common.nowMs()
            )
            requestRepository.insertDraft(draft)
            onCreated(draft.id)
        }
    }

    fun observeCollections(onCollectionsChanged: (List<Collection>) -> Unit) {
        collectionsJob?.cancel()
        collectionsJob = collectionRepository.getAllCollections()
            .onEach { onCollectionsChanged(it) }
            .launchIn(scope)
    }

    fun observeRequests(onRequestsChanged: (List<Request>) -> Unit) {
        requestsJob?.cancel()
        requestsJob = requestRepository.getAllRequests()
            .onEach { onRequestsChanged(it) }
            .launchIn(scope)
    }

    fun createCollection(
        name: String,
        description: String? = null,
        onCreated: ((Collection) -> Unit)? = null
    ) {
        scope.launch {
            val now = com.learn.reqlite.utils.common.nowMs()
            val col = Collection(
                id = "col_${now}_${(1000..9999).random()}",
                name = name.trim(),
                description = description?.trim(),
                createdAt = now,
                updatedAt = now
            )
            collectionRepository.insertCollection(col)
            onCreated?.invoke(col)
        }
    }

    fun deleteCollection(id: String) {
        scope.launch {
            val col = collectionRepository.getCollectionById(id)
            if (col != null) {
                collectionRepository.deleteCollection(col)
            }
        }
    }

    fun saveRequestToCollection(
        collectionId: String,
        name: String,
        methodName: String,
        url: String,
        headers: List<RequestField> = emptyList(),
        queryParams: List<RequestField> = emptyList(),
        bodyContent: String? = null,
        isGraphQL: Boolean = false,
        variablesContent: String? = null,
        onSaved: ((Request) -> Unit)? = null
    ) {
        scope.launch {
            val method = when (methodName.uppercase()) {
                "POST" -> HttpMethod.POST
                "PUT" -> HttpMethod.PUT
                "DELETE" -> HttpMethod.DELETE
                "PATCH" -> HttpMethod.PATCH
                "HEAD" -> HttpMethod.HEAD
                "OPTIONS" -> HttpMethod.OPTIONS
                else -> HttpMethod.GET
            }
            val requestBody = when {
                isGraphQL -> RequestBody.GraphQLBody(bodyContent ?: "", variablesContent ?: "{}")
                !bodyContent.isNullOrBlank() -> RequestBody.TextBody(bodyContent, "application/json")
                else -> RequestBody.NoBody
            }
            val now = com.learn.reqlite.utils.common.nowMs()
            val req = Request(
                id = "req_${now}_${(1000..9999).random()}",
                collectionId = collectionId,
                folderId = null,
                name = name.ifBlank { url },
                method = method,
                url = url,
                headers = headers,
                queryParams = queryParams,
                body = requestBody,
                createdAt = now,
                updatedAt = now
            )
            requestRepository.insertRequest(req)
            onSaved?.invoke(req)
        }
    }

    fun updateRequest(
        requestId: String,
        name: String? = null,
        methodName: String,
        url: String,
        headers: List<RequestField> = emptyList(),
        queryParams: List<RequestField> = emptyList(),
        bodyContent: String? = null,
        isGraphQL: Boolean = false,
        variablesContent: String? = null,
        onUpdated: ((Request) -> Unit)? = null
    ) {
        scope.launch {
            val existing = requestRepository.getRequestById(requestId)
            val method = when (methodName.uppercase()) {
                "POST" -> HttpMethod.POST
                "PUT" -> HttpMethod.PUT
                "DELETE" -> HttpMethod.DELETE
                "PATCH" -> HttpMethod.PATCH
                "HEAD" -> HttpMethod.HEAD
                "OPTIONS" -> HttpMethod.OPTIONS
                else -> HttpMethod.GET
            }
            val requestBody = when {
                isGraphQL -> RequestBody.GraphQLBody(bodyContent ?: "", variablesContent ?: "{}")
                !bodyContent.isNullOrBlank() -> RequestBody.TextBody(bodyContent, "application/json")
                else -> RequestBody.NoBody
            }
            val now = com.learn.reqlite.utils.common.nowMs()
            val req = (existing?.copy(
                method = method,
                url = url,
                headers = headers,
                queryParams = queryParams,
                body = requestBody,
                updatedAt = now
            )) ?: Request(
                id = requestId,
                collectionId = "default",
                folderId = null,
                name = name ?: url,
                method = method,
                url = url,
                headers = headers,
                queryParams = queryParams,
                body = requestBody,
                createdAt = now,
                updatedAt = now
            )
            requestRepository.insertRequest(req)
            onUpdated?.invoke(req)
        }
    }

    fun deleteRequest(id: String) {
        scope.launch {
            requestRepository.deleteRequest(id)
        }
    }

    fun createRequestField(key: String, value: String, isEnabled: Boolean): RequestField {
        return RequestField(
            id = "fld_${com.learn.reqlite.utils.common.nowMs()}_${(1000..9999).random()}",
            key = key,
            value = value,
            isEnabled = isEnabled,
            description = null
        )
    }

    fun extractRequestBodyText(request: Request): String {
        return when (val b = request.body) {
            is RequestBody.TextBody -> b.content
            is RequestBody.GraphQLBody -> b.query
            is RequestBody.UrlEncodedBody -> b.fields.filter { it.isEnabled }.joinToString("&") { "${it.key}=${it.value}" }
            is RequestBody.FormDataBody -> b.parts.filter { it.isEnabled }.joinToString("&") { "${it.key}=${it.value}" }
            else -> ""
        }
    }

    fun isGraphQLRequest(request: Request): Boolean = request.body is RequestBody.GraphQLBody

    fun extractGraphQLVariables(request: Request): String {
        return (request.body as? RequestBody.GraphQLBody)?.variables ?: "{}"
    }

    fun formatGraphQL(query: String): String = GraphQLFormatter.format(query)

    fun parseGraphQLSchema(json: String): GraphQLSchema? {
        return try {
            GraphQLIntrospectionParser.parse(json)
        } catch (_: Exception) {
            null
        }
    }

    fun getGraphQLCompletions(query: String, cursorPosition: Int, schema: GraphQLSchema?): List<GraphQLCompletionItem> {
        return GraphQLAutocompleteEngine.getCompletions(query, cursorPosition, schema)
    }

    fun filterGraphQLSchema(schema: GraphQLSchema, searchQuery: String): List<SchemaExplorerCategory> {
        return GraphQLSchemaExplorer.filter(schema, searchQuery)
    }

    fun generateGraphQLSnippet(item: SchemaExplorerItem, schema: GraphQLSchema): String? {
        val field = item.rawField ?: return null
        val operation = when (item.kind) {
            SchemaItemKind.MUTATION -> "mutation"
            SchemaItemKind.SUBSCRIPTION -> "subscription"
            else -> "query"
        }
        return GraphQLSchemaExplorer.generateSnippet(field, schema, operation)
    }

    fun getIntrospectionQuery(): String = GraphQLIntrospectionParser.INTROSPECTION_QUERY

    fun introspectGraphQL(
        url: String,
        environmentId: String?,
        headers: List<RequestField> = emptyList(),
        onSuccess: (GraphQLSchema) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                val draft = Draft(
                    id = "ios_introspect_${com.learn.reqlite.utils.common.nowMs()}_${(1000..9999).random()}",
                    requestId = null,
                    method = HttpMethod.POST,
                    url = url,
                    headers = headers,
                    queryParams = emptyList(),
                    body = RequestBody.GraphQLBody(
                        query = GraphQLIntrospectionParser.INTROSPECTION_QUERY,
                        variables = "{}"
                    ),
                    updatedAt = com.learn.reqlite.utils.common.nowMs()
                )
                requestRepository.insertDraft(draft)
                val result = executionEngine.executeWithHeaders(draft.id, environmentId)
                val schema = GraphQLIntrospectionParser.parse(result.responseBody)
                onSuccess(schema)
            } catch (e: Exception) {
                onError(e.message ?: "Introspection failed")
            }
        }
    }

    fun previewPostmanCollection(
        jsonContent: String,
        onSuccess: (collectionName: String, requestsCount: Int, foldersCount: Int) -> Unit,
        onError: (String) -> Unit
    ) {
        when (val result = postmanParser.parse(jsonContent)) {
            is PostmanParseResult.Error -> onError(result.message)
            is PostmanParseResult.Success -> {
                onSuccess(result.collection.name, result.requests.size, result.folders.size)
            }
        }
    }

    fun importPostmanCollection(
        jsonContent: String,
        onSuccess: (collectionName: String, requestsCount: Int) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            val parsedName = when (val parsed = postmanParser.parse(jsonContent)) {
                is PostmanParseResult.Success -> parsed.collection.name
                else -> "Imported Collection"
            }
            when (val result = workspaceImporter.importPostmanCollection(jsonContent)) {
                is WorkspaceImportResult.Error -> onError(result.message)
                is WorkspaceImportResult.Success -> {
                    onSuccess(parsedName, result.requestsImported)
                }
            }
        }
    }

    fun previewInsomniaCollection(
        jsonContent: String,
        onSuccess: (collectionName: String, requestsCount: Int, foldersCount: Int) -> Unit,
        onError: (String) -> Unit
    ) {
        when (val result = insomniaParser.parse(jsonContent)) {
            is InsomniaParseResult.Error -> onError(result.message)
            is InsomniaParseResult.Success -> {
                onSuccess(result.collection.name, result.requests.size, result.folders.size)
            }
        }
    }

    fun importInsomniaCollection(
        jsonContent: String,
        onSuccess: (collectionName: String, requestsCount: Int) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            val parsedName = when (val parsed = insomniaParser.parse(jsonContent)) {
                is InsomniaParseResult.Success -> parsed.collection.name
                else -> "Imported Insomnia Collection"
            }
            when (val result = workspaceImporter.importInsomniaCollection(jsonContent)) {
                is WorkspaceImportResult.Error -> onError(result.message)
                is WorkspaceImportResult.Success -> {
                    onSuccess(parsedName, result.requestsImported)
                }
            }
        }
    }

    fun previewUniversal(
        jsonContent: String,
        onSuccess: (format: String, collectionName: String, requestsCount: Int, foldersCount: Int) -> Unit,
        onError: (String) -> Unit
    ) {
        if (jsonContent.contains("\"_type\"") && (jsonContent.contains("\"export_format\"") || jsonContent.contains("\"resources\""))) {
            when (val res = insomniaParser.parse(jsonContent)) {
                is InsomniaParseResult.Success -> {
                    onSuccess("Insomnia", res.collection.name, res.requests.size, res.folders.size)
                    return
                }
                is InsomniaParseResult.Error -> {
                    onError(res.message)
                    return
                }
            }
        }

        if (jsonContent.contains("\"info\"") && (jsonContent.contains("\"item\"") || jsonContent.contains("\"schema\""))) {
            when (val res = postmanParser.parse(jsonContent)) {
                is PostmanParseResult.Success -> {
                    onSuccess("Postman", res.collection.name, res.requests.size, res.folders.size)
                    return
                }
                is PostmanParseResult.Error -> {
                    onError(res.message)
                    return
                }
            }
        }

        if (jsonContent.contains("\"collections\"") || jsonContent.contains("\"schemaVersion\"")) {
            onSuccess("ReqLite Workspace", "ReqLite Workspace", 0, 0)
            return
        }

        // Try fallbacks
        when (val insRes = insomniaParser.parse(jsonContent)) {
            is InsomniaParseResult.Success -> {
                onSuccess("Insomnia", insRes.collection.name, insRes.requests.size, insRes.folders.size)
                return
            }
            is InsomniaParseResult.Error -> {
                when (val pmRes = postmanParser.parse(jsonContent)) {
                    is PostmanParseResult.Success -> {
                        onSuccess("Postman", pmRes.collection.name, pmRes.requests.size, pmRes.folders.size)
                        return
                    }
                    is PostmanParseResult.Error -> {
                        onError("Unrecognized collection format: ${pmRes.message}")
                    }
                }
            }
        }
    }

    fun importUniversal(
        jsonContent: String,
        onSuccess: (collectionsCount: Int, requestsCount: Int, environmentsCount: Int) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            when (val result = workspaceImporter.importUniversal(jsonContent)) {
                is WorkspaceImportResult.Error -> onError(result.message)
                is WorkspaceImportResult.Success -> {
                    onSuccess(result.collectionsImported, result.requestsImported, result.environmentsImported)
                }
            }
        }
    }

    fun exportWorkspace(
        collectionIds: List<String>? = null,
        environmentIds: List<String>? = null,
        includeSecrets: Boolean = false,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                val json = workspaceExporter.exportWorkspace(
                    collectionIds = collectionIds,
                    environmentIds = environmentIds,
                    includeSecrets = includeSecrets
                )
                onSuccess(json)
            } catch (e: Exception) {
                onError(e.message ?: "Export failed")
            }
        }
    }

    fun exportCollection(
        collectionId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                val json = workspaceExporter.exportCollection(collectionId)
                onSuccess(json)
            } catch (e: Exception) {
                onError(e.message ?: "Export collection failed")
            }
        }
    }

    fun close() {
        envJob?.cancel()
        historyJob?.cancel()
        collectionsJob?.cancel()
        requestsJob?.cancel()
        executionJob?.cancel()
    }
}
