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
import com.learn.reqlite.domain.export.WorkspaceImportResult
import com.learn.reqlite.domain.parser.PostmanCollectionParser
import com.learn.reqlite.domain.parser.PostmanParseResult
import com.learn.reqlite.domain.parser.SmartPayload
import com.learn.reqlite.domain.parser.SmartPayloadParser
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
    postmanParser: PostmanCollectionParser? = null,
    private val scope: CoroutineScope
) {
    private val postmanParser: PostmanCollectionParser = postmanParser ?: com.learn.reqlite.domain.parser.PostmanCollectionParserImpl()
    private val workspaceImporter: WorkspaceImporter = workspaceImporter ?: WorkspaceExportImportManager(
        collectionRepository = collectionRepository,
        requestRepository = requestRepository,
        environmentRepository = environmentRepository,
        postmanParser = this.postmanParser
    )

    constructor() : this(
        executionEngine = DiHelper.getRequestExecutionEngine(),
        environmentRepository = DiHelper.getEnvironmentRepository(),
        requestRepository = DiHelper.getRequestRepository(),
        historyRepository = DiHelper.getHistoryRepository(),
        collectionRepository = DiHelper.getCollectionRepository(),
        workspaceImporter = DiHelper.getWorkspaceImporter(),
        postmanParser = DiHelper.getPostmanCollectionParser(),
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
            val requestBody = if (!bodyContent.isNullOrBlank()) {
                RequestBody.TextBody(bodyContent, "application/json")
            } else {
                RequestBody.NoBody
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
            val requestBody = if (!bodyContent.isNullOrBlank()) {
                RequestBody.TextBody(bodyContent, "application/json")
            } else {
                RequestBody.NoBody
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
            is RequestBody.UrlEncodedBody -> b.fields.filter { it.isEnabled }.joinToString("&") { "${it.key}=${it.value}" }
            is RequestBody.FormDataBody -> b.parts.filter { it.isEnabled }.joinToString("&") { "${it.key}=${it.value}" }
            else -> ""
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
            when (val result = workspaceImporter.importPostmanCollection(jsonContent)) {
                is WorkspaceImportResult.Error -> onError(result.message)
                is WorkspaceImportResult.Success -> {
                    onSuccess("Imported Collection", result.requestsImported)
                }
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
