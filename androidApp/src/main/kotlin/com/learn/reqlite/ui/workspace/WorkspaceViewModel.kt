package com.learn.reqlite.ui.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learn.reqlite.domain.engine.RequestExecutionEngine
import com.learn.reqlite.domain.graphql.model.GraphQLSchema
import com.learn.reqlite.domain.graphql.parser.GraphQLIntrospectionParser
import com.learn.reqlite.domain.model.*
import com.learn.reqlite.domain.repository.CollectionRepository
import com.learn.reqlite.domain.repository.EnvironmentRepository
import com.learn.reqlite.domain.repository.HistoryRepository
import com.learn.reqlite.domain.repository.RequestRepository
import com.learn.reqlite.domain.repository.SecureStorage
import com.learn.reqlite.domain.usecase.VariableResolver
import com.learn.reqlite.ui.environment.EnvironmentUiModel
import com.learn.reqlite.ui.response.HttpResponseUiModel
import com.learn.reqlite.utils.common.nowMs
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod as KtorHttpMethod
import io.ktor.util.encodeBase64
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

data class PendingExecutionRequest(
    val draftId: String,
    val method: String,
    val url: String,
    val environment: EnvironmentUiModel
)

class WorkspaceViewModel(
    private val executionEngine: RequestExecutionEngine? = null,
    private val environmentRepository: EnvironmentRepository? = null,
    private val historyRepository: HistoryRepository? = null,
    private val requestRepository: RequestRepository? = null,
    private val collectionRepository: CollectionRepository? = null,
    private val httpClient: HttpClient? = null,
    private val secureStorage: SecureStorage? = null,
    private val variableResolver: VariableResolver = VariableResolver()
) : ViewModel() {

    val collections: StateFlow<List<com.learn.reqlite.domain.model.Collection>> = (collectionRepository?.getAllCollections() ?: emptyFlow())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _method = MutableStateFlow("GET")
    val method: StateFlow<String> = _method.asStateFlow()

    private val _queryParams = MutableStateFlow<List<RequestField>>(emptyList())
    val queryParams: StateFlow<List<RequestField>> = _queryParams.asStateFlow()

    private val _headers = MutableStateFlow<List<RequestField>>(emptyList())
    val headers: StateFlow<List<RequestField>> = _headers.asStateFlow()

    private val _body = MutableStateFlow<RequestBody>(RequestBody.NoBody)
    val body: StateFlow<RequestBody> = _body.asStateFlow()

    private val _authConfiguration = MutableStateFlow<AuthConfiguration>(AuthConfiguration.None)
    val authConfiguration: StateFlow<AuthConfiguration> = _authConfiguration.asStateFlow()

    private val _preRequestScript = MutableStateFlow("")
    val preRequestScript: StateFlow<String> = _preRequestScript.asStateFlow()

    private val _testScript = MutableStateFlow("")
    val testScript: StateFlow<String> = _testScript.asStateFlow()

    private val _testResults = MutableStateFlow<List<com.learn.reqlite.domain.script.model.TestAssertionResult>>(emptyList())
    val testResults: StateFlow<List<com.learn.reqlite.domain.script.model.TestAssertionResult>> = _testResults.asStateFlow()

    private val _scriptLogs = MutableStateFlow<List<String>>(emptyList())
    val scriptLogs: StateFlow<List<String>> = _scriptLogs.asStateFlow()

    private val _executionState = MutableStateFlow<ExecutionUiState>(ExecutionUiState.Idle)
    val executionState: StateFlow<ExecutionUiState> = _executionState.asStateFlow()

    private val _environments = MutableStateFlow<List<EnvironmentUiModel>>(emptyList())
    val environments: StateFlow<List<EnvironmentUiModel>> = _environments.asStateFlow()

    private val _activeEnvironment = MutableStateFlow<EnvironmentUiModel?>(null)
    val activeEnvironment: StateFlow<EnvironmentUiModel?> = _activeEnvironment.asStateFlow()

    private val _isProtectedWarningVisible = MutableStateFlow(false)
    val isProtectedWarningVisible: StateFlow<Boolean> = _isProtectedWarningVisible.asStateFlow()

    private val _pendingExecution = MutableStateFlow<PendingExecutionRequest?>(null)
    val pendingExecution: StateFlow<PendingExecutionRequest?> = _pendingExecution.asStateFlow()

    private val _openDrafts = MutableStateFlow<List<Draft>>(emptyList())
    val openDrafts: StateFlow<List<Draft>> = _openDrafts.asStateFlow()

    private val _activeDraftId = MutableStateFlow<String?>(null)
    val activeDraftId: StateFlow<String?> = _activeDraftId.asStateFlow()

    private val _activeRequestId = MutableStateFlow<String?>(null)
    val activeRequestId: StateFlow<String?> = _activeRequestId.asStateFlow()

    private val _graphQLSchema = MutableStateFlow<GraphQLSchema?>(null)
    val graphQLSchema: StateFlow<GraphQLSchema?> = _graphQLSchema.asStateFlow()

    private val _isLoadingSchema = MutableStateFlow(false)
    val isLoadingSchema: StateFlow<Boolean> = _isLoadingSchema.asStateFlow()

    private var activeExecutionJob: Job? = null

    init {
        loadEnvironments()
        observeDrafts()
    }

    private fun observeDrafts() {
        if (requestRepository != null) {
            viewModelScope.launch {
                requestRepository.getAllDrafts().collect { drafts ->
                    _openDrafts.value = drafts
                }
            }
        }
    }

    fun setInitialState(
        initialUrl: String? = null,
        initialMethod: String = "GET",
        initialHeaders: List<RequestField> = emptyList(),
        initialQueryParams: List<RequestField> = emptyList(),
        initialBody: RequestBody = RequestBody.NoBody,
        initialAuth: AuthConfiguration = AuthConfiguration.None
    ) {
        if (!initialUrl.isNullOrBlank()) _url.value = initialUrl
        _method.value = initialMethod
        if (initialHeaders.isNotEmpty()) _headers.value = initialHeaders
        if (initialQueryParams.isNotEmpty()) _queryParams.value = initialQueryParams
        _body.value = initialBody
        _authConfiguration.value = initialAuth
    }

    fun setUrl(newUrl: String) {
        _url.value = newUrl
        saveDraft()
    }

    fun setMethod(newMethod: String) {
        _method.value = newMethod
        saveDraft()
    }

    fun setQueryParams(newParams: List<RequestField>) {
        _queryParams.value = newParams
        saveDraft()
    }

    fun setHeaders(newHeaders: List<RequestField>) {
        _headers.value = newHeaders
        saveDraft()
    }

    fun setBody(newBody: RequestBody) {
        _body.value = newBody
        saveDraft()
    }

    fun setAuthConfiguration(newAuth: AuthConfiguration) {
        _authConfiguration.value = newAuth
    }

    fun setPreRequestScript(script: String) {
        _preRequestScript.value = script
        saveDraft()
    }

    fun setTestScript(script: String) {
        _testScript.value = script
        saveDraft()
    }

    fun insertPreRequestSnippet(code: String) {
        val current = _preRequestScript.value
        _preRequestScript.value = if (current.isBlank()) code else "$current\n\n$code"
        saveDraft()
    }

    fun insertTestSnippet(code: String) {
        val current = _testScript.value
        _testScript.value = if (current.isBlank()) code else "$current\n\n$code"
        saveDraft()
    }

    val preRequestSnippets: List<com.learn.reqlite.domain.script.model.ScriptSnippet> = com.learn.reqlite.domain.script.model.ScriptSnippets.preRequestSnippets
    val testSnippets: List<com.learn.reqlite.domain.script.model.ScriptSnippet> = com.learn.reqlite.domain.script.model.ScriptSnippets.testSnippets

    private fun loadEnvironments() {
        if (environmentRepository != null) {
            viewModelScope.launch {
                environmentRepository.getAllEnvironments().collect { list ->
                    val uiModels = list.map { EnvironmentUiModel.fromDomain(it) }
                    _environments.value = uiModels
                    _activeEnvironment.value?.let { current ->
                        _activeEnvironment.value = uiModels.find { it.id == current.id }
                    }
                }
            }
        }
    }

    fun setEnvironments(list: List<EnvironmentUiModel>) {
        _environments.value = list
    }

    fun selectEnvironment(env: EnvironmentUiModel?) {
        _activeEnvironment.value = env
    }

    fun onSendClicked(
        draftId: String = "draft_${nowMs()}",
        method: String = _method.value,
        url: String = _url.value
    ) {
        val currentEnv = _activeEnvironment.value
        if (currentEnv != null && currentEnv.isProtected && currentEnv.requireConfirmation) {
            _pendingExecution.value = PendingExecutionRequest(
                draftId = draftId,
                method = method,
                url = url,
                environment = currentEnv
            )
            _isProtectedWarningVisible.value = true
        } else {
            if (executionEngine != null && _url.value.isBlank()) {
                executeRequest(draftId, currentEnv?.id)
            } else {
                executeDirectRequest()
            }
        }
    }

    fun confirmProtectedExecution() {
        val pending = _pendingExecution.value
        _isProtectedWarningVisible.value = false
        _pendingExecution.value = null
        if (pending != null && executionEngine != null && _url.value.isBlank()) {
            executeRequest(pending.draftId, pending.environment.id)
        } else {
            executeDirectRequest()
        }
    }

    fun dismissProtectedWarning() {
        _isProtectedWarningVisible.value = false
        _pendingExecution.value = null
    }

    fun executeDirectRequest() {
        activeExecutionJob?.cancel()
        activeExecutionJob = viewModelScope.launch {
            _executionState.value = ExecutionUiState.Loading(progress = null)

            val currentUrl = _url.value.trim()
            if (currentUrl.isBlank()) {
                _executionState.value = ExecutionUiState.Error("URL cannot be empty")
                return@launch
            }

            val envVariables = _activeEnvironment.value?.id?.let { envId ->
                environmentRepository?.getEnvironmentById(envId)?.variables
            } ?: emptyList()

            val resolvedVariables = envVariables.map { variable ->
                if (variable.isSecret && secureStorage != null) {
                    val secretVal = secureStorage.getSecret("env_var_${variable.id}")
                    variable.copy(value = secretVal ?: "")
                } else {
                    variable
                }
            }

            val scriptEngine = com.learn.reqlite.domain.script.ScriptInterpreter()
            val mutableVariablesMap = resolvedVariables.associate { it.key to it.value }.toMutableMap()
            var effectiveUrl = currentUrl
            val dynamicHeaders = mutableMapOf<String, String>()
            val directScriptLogs = mutableListOf<String>()

            // Execute Pre-request Script if present
            val preScript = _preRequestScript.value
            val effectiveHeaders = _headers.value.filter { it.isEnabled }.associate { it.key to it.value }.toMutableMap()
            if (preScript.isNotBlank()) {
                val preContext = com.learn.reqlite.domain.script.PreRequestContext(
                    url = currentUrl,
                    method = _method.value,
                    headers = effectiveHeaders,
                    queryParams = _queryParams.value.filter { it.isEnabled }.associate { it.key to it.value },
                    bodyContent = when (val b = _body.value) {
                        is RequestBody.TextBody -> b.content
                        else -> null
                    },
                    environmentVariables = mutableVariablesMap
                )
                val preResult = scriptEngine.executePreRequest(preScript, preContext)
                directScriptLogs.addAll(preResult.logs)
                if (preResult.mutatedVariables.isNotEmpty()) {
                    mutableVariablesMap.putAll(preResult.mutatedVariables)
                }
                if (preResult.mutatedHeaders.isNotEmpty()) {
                    effectiveHeaders.putAll(preResult.mutatedHeaders)
                }
                if (!preResult.mutatedUrl.isNullOrBlank()) {
                    effectiveUrl = preResult.mutatedUrl!!
                }
            }

            val effectiveVariables = resolvedVariables.map { v ->
                val updated = mutableVariablesMap[v.key] ?: v.value
                v.copy(value = updated)
            } + mutableVariablesMap.filterKeys { k -> resolvedVariables.none { it.key == k } }.map { (k, v) ->
                Variable(
                    id = "var_${k}",
                    key = k,
                    value = v
                )
            }

            val resolvedUrl = variableResolver.resolve(effectiveUrl, effectiveVariables)
            val client = httpClient ?: HttpClient()
            val startTime = nowMs()

            try {
                val statement = client.request(resolvedUrl) {
                    this.method = KtorHttpMethod.parse(_method.value)

                    // Inject Headers (including pre-request mutated headers)
                    effectiveHeaders.forEach { (k, v) ->
                        val resK = variableResolver.resolve(k, effectiveVariables)
                        val resV = variableResolver.resolve(v, effectiveVariables)
                        if (resK.isNotBlank()) {
                            header(resK, resV)
                        }
                    }

                    // Inject Auth
                    when (val auth = _authConfiguration.value) {
                        is AuthConfiguration.None -> {}
                        is AuthConfiguration.Bearer -> {
                            val resolvedToken = variableResolver.resolve(auth.token, effectiveVariables)
                            header(HttpHeaders.Authorization, "Bearer $resolvedToken")
                        }
                        is AuthConfiguration.Basic -> {
                            val u = variableResolver.resolve(auth.username, effectiveVariables)
                            val p = variableResolver.resolve(auth.password, effectiveVariables)
                            val creds = "$u:$p".encodeBase64()
                            header(HttpHeaders.Authorization, "Basic $creds")
                        }
                    }

                    // Inject Query Params
                    _queryParams.value.filter { it.isEnabled }.forEach { qp ->
                        val k = variableResolver.resolve(qp.key, effectiveVariables)
                        val v = variableResolver.resolve(qp.value, effectiveVariables)
                        if (k.isNotBlank()) {
                            parameter(k, v)
                        }
                    }

                    // Inject Body
                    when (val reqBody = _body.value) {
                        is RequestBody.NoBody -> {}
                        is RequestBody.TextBody -> {
                            val resolvedContent = variableResolver.resolve(reqBody.content, effectiveVariables)
                            val resolvedContentType = variableResolver.resolve(reqBody.contentType, effectiveVariables)
                            header(HttpHeaders.ContentType, resolvedContentType.ifBlank { "application/json" })
                            setBody(resolvedContent)
                        }
                        is RequestBody.FormDataBody -> {}
                        is RequestBody.UrlEncodedBody -> {
                            val pairs = reqBody.fields.filter { it.isEnabled }.map {
                                val k = variableResolver.resolve(it.key, effectiveVariables)
                                val v = variableResolver.resolve(it.value, effectiveVariables)
                                "$k=$v"
                            }.joinToString("&")
                            header(HttpHeaders.ContentType, "application/x-www-form-urlencoded")
                            setBody(pairs)
                        }
                        is RequestBody.GraphQLBody -> {
                            val resolvedQuery = variableResolver.resolve(reqBody.query, effectiveVariables)
                            val resolvedVariablesJson = variableResolver.resolve(reqBody.variables, effectiveVariables)
                            val payload = buildJsonObject {
                                put("query", resolvedQuery)
                                if (resolvedVariablesJson.isNotBlank()) {
                                    val parsedVars = try {
                                        Json.parseToJsonElement(resolvedVariablesJson)
                                    } catch (_: Exception) {
                                        null
                                    }
                                    if (parsedVars != null) {
                                        put("variables", parsedVars)
                                    }
                                }
                                if (!reqBody.operationName.isNullOrBlank()) {
                                    put("operationName", reqBody.operationName)
                                }
                            }.toString()
                            header(HttpHeaders.ContentType, "application/json")
                            setBody(payload)
                        }
                    }
                }

                val responseBody = statement.bodyAsText()
                val statusCode = statement.status.value
                val durationMs = nowMs() - startTime
                val contentType = statement.headers[HttpHeaders.ContentType] ?: "text/plain"
                val responseHeadersList = statement.headers.entries().flatMap { entry ->
                    entry.value.map { v -> Pair(entry.key, v) }
                }
                val bodyBytes = responseBody.encodeToByteArray().size.toLong()

                // Execute Post-request Test Script if present
                val tScript = _testScript.value
                val directTestResults = mutableListOf<com.learn.reqlite.domain.script.model.TestAssertionResult>()
                if (tScript.isNotBlank()) {
                    val postContext = com.learn.reqlite.domain.script.PostRequestContext(
                        statusCode = statusCode,
                        statusText = statement.status.description,
                        responseTimeMs = durationMs,
                        responseHeaders = responseHeadersList.toMap(),
                        responseBody = responseBody,
                        requestUrl = resolvedUrl,
                        requestMethod = _method.value,
                        environmentVariables = mutableVariablesMap
                    )
                    val postResult = scriptEngine.executePostRequestTests(tScript, postContext)
                    directTestResults.addAll(postResult.testResults)
                    directScriptLogs.addAll(postResult.logs)
                    if (postResult.mutatedVariables.isNotEmpty()) {
                        _activeEnvironment.value?.id?.let { envId ->
                            environmentRepository?.let { repo ->
                                viewModelScope.launch {
                                    val env = repo.getEnvironmentById(envId)
                                    if (env != null) {
                                        val existingVars = env.variables.toMutableList()
                                        postResult.mutatedVariables.forEach { (k, v) ->
                                            val idx = existingVars.indexOfFirst { it.key == k }
                                            if (idx >= 0) {
                                                existingVars[idx] = existingVars[idx].copy(value = v)
                                            } else {
                                                existingVars.add(Variable(id = "var_${k}", key = k, value = v))
                                            }
                                        }
                                        repo.insertEnvironment(env.copy(variables = existingVars))
                                    }
                                }
                            }
                        }
                    }
                }

                _testResults.value = directTestResults
                _scriptLogs.value = directScriptLogs

                val historyEntry = HistoryEntry(
                    id = "hist_${nowMs()}",
                    requestId = null,
                    requestMethod = parseHttpMethod(_method.value),
                    requestUrl = resolvedUrl,
                    timestamp = nowMs(),
                    statusCode = statusCode,
                    durationMs = durationMs,
                    responseArtifactId = null
                )
                try {
                    historyRepository?.insertHistoryEntry(historyEntry)
                } catch (_: Exception) {}

                val uiModel = HttpResponseUiModel(
                    statusCode = statusCode,
                    statusText = statement.status.description,
                    durationMs = durationMs,
                    sizeBytes = bodyBytes,
                    contentType = contentType,
                    headers = responseHeadersList,
                    body = responseBody,
                    timestamp = nowMs(),
                    url = resolvedUrl,
                    method = _method.value,
                    artifactId = null,
                    testResults = directTestResults,
                    scriptLogs = directScriptLogs
                )

                _executionState.value = ExecutionUiState.Success(
                    historyEntry = historyEntry,
                    response = uiModel
                )

            } catch (e: Exception) {
                val durationMs = nowMs() - startTime
                val errorMsg = e.message ?: "Unknown network error"
                val failedEntry = HistoryEntry(
                    id = "hist_${nowMs()}",
                    requestId = null,
                    requestMethod = parseHttpMethod(_method.value),
                    requestUrl = resolvedUrl,
                    timestamp = nowMs(),
                    durationMs = durationMs,
                    errorCode = "NETWORK_ERROR",
                    errorMessage = errorMsg
                )
                try {
                    historyRepository?.insertHistoryEntry(failedEntry)
                } catch (_: Exception) {}
                _executionState.value = ExecutionUiState.Error(errorMsg)
            }
        }
    }

    fun executeRequest(draftId: String, environmentId: String? = null) {
        if (executionEngine != null && _url.value.isBlank()) {
            activeExecutionJob?.cancel()
            activeExecutionJob = viewModelScope.launch {
                _executionState.value = ExecutionUiState.Loading(progress = null)
                try {
                    val result = executionEngine.executeWithHeaders(draftId, environmentId)
                    _testResults.value = result.testResults
                    _scriptLogs.value = result.scriptLogs
                    val uiModel = HttpResponseUiModel(
                        statusCode = result.historyEntry.statusCode ?: 200,
                        statusText = "OK",
                        durationMs = result.historyEntry.durationMs ?: 0L,
                        sizeBytes = result.responseBody.encodeToByteArray().size.toLong(),
                        contentType = "application/json",
                        headers = result.responseHeaders.map { Pair(it.key, it.value) },
                        body = result.responseBody,
                        timestamp = result.historyEntry.timestamp,
                        url = result.historyEntry.requestUrl,
                        method = result.historyEntry.requestMethod.name,
                        artifactId = result.historyEntry.responseArtifactId,
                        testResults = result.testResults,
                        scriptLogs = result.scriptLogs
                    )
                    _executionState.value = ExecutionUiState.Success(
                        historyEntry = result.historyEntry,
                        response = uiModel
                    )
                } catch (e: Exception) {
                    _executionState.value = ExecutionUiState.Error(e.message ?: "Unknown error")
                }
            }
        } else {
            executeDirectRequest()
        }
    }

    fun resetExecutionState() {
        _executionState.value = ExecutionUiState.Idle
    }

    fun cancelExecution() {
        activeExecutionJob?.cancel()
        _executionState.value = ExecutionUiState.Idle
    }

    fun saveDraft(draftId: String = _activeDraftId.value ?: "draft_${nowMs()}") {
        viewModelScope.launch {
            val draft = Draft(
                id = draftId,
                requestId = _activeRequestId.value,
                method = parseHttpMethod(_method.value),
                url = _url.value,
                headers = _headers.value,
                queryParams = _queryParams.value,
                body = _body.value,
                preRequestScript = _preRequestScript.value.takeIf { it.isNotBlank() },
                testScript = _testScript.value.takeIf { it.isNotBlank() },
                updatedAt = nowMs()
            )
            requestRepository?.insertDraft(draft)
            _activeDraftId.value = draftId
        }
    }

    fun loadDraft(draftId: String) {
        viewModelScope.launch {
            val draft = requestRepository?.getDraftById(draftId) ?: return@launch
            _url.value = draft.url
            _method.value = draft.method.name
            _headers.value = draft.headers
            _queryParams.value = draft.queryParams
            _body.value = draft.body
            _preRequestScript.value = draft.preRequestScript ?: ""
            _testScript.value = draft.testScript ?: ""
            _activeDraftId.value = draftId
            _activeRequestId.value = draft.requestId
        }
    }

    fun switchTab(draftId: String) {
        saveDraft() // Save current state
        loadDraft(draftId)
    }

    fun createNewTab() {
        saveDraft() // Save current state
        val newDraftId = "draft_${nowMs()}"
        _activeRequestId.value = null
        _url.value = ""
        _method.value = "GET"
        _headers.value = emptyList()
        _queryParams.value = emptyList()
        _body.value = RequestBody.NoBody
        _preRequestScript.value = ""
        _testScript.value = ""
        _testResults.value = emptyList()
        _scriptLogs.value = emptyList()
        _activeDraftId.value = newDraftId
        saveDraft(newDraftId)
    }

    fun applySmartPayload(rawInput: String): Boolean {
        val parser = com.learn.reqlite.domain.parser.SmartPayloadParser()
        return when (val parsed = parser.parse(rawInput)) {
            is com.learn.reqlite.domain.parser.SmartPayload.RequestConfig -> {
                _url.value = parsed.url
                _method.value = parsed.method.name
                _headers.value = parsed.headers
                _queryParams.value = parsed.queryParams
                _body.value = parsed.body
                val token = parsed.bearerToken
                if (token != null) {
                    _authConfiguration.value = AuthConfiguration.Bearer(token)
                }
                saveDraft()
                true
            }
            is com.learn.reqlite.domain.parser.SmartPayload.AuthToken -> {
                _authConfiguration.value = AuthConfiguration.Bearer(parsed.token)
                true
            }
            is com.learn.reqlite.domain.parser.SmartPayload.PlainUrl -> {
                _url.value = parsed.url
                true
            }
            is com.learn.reqlite.domain.parser.SmartPayload.Error -> {
                false
            }
        }
    }

    fun applyCurl(curlCommand: String): Boolean = applySmartPayload(curlCommand)

    private fun parseHttpMethod(name: String): com.learn.reqlite.domain.model.HttpMethod {
        return when (name.uppercase()) {
            "POST" -> com.learn.reqlite.domain.model.HttpMethod.POST
            "PUT" -> com.learn.reqlite.domain.model.HttpMethod.PUT
            "DELETE" -> com.learn.reqlite.domain.model.HttpMethod.DELETE
            "PATCH" -> com.learn.reqlite.domain.model.HttpMethod.PATCH
            "HEAD" -> com.learn.reqlite.domain.model.HttpMethod.HEAD
            "OPTIONS" -> com.learn.reqlite.domain.model.HttpMethod.OPTIONS
            else -> com.learn.reqlite.domain.model.HttpMethod.GET
        }
    }

    fun saveCurrentRequest(onSaved: (Request) -> Unit = {}) {
        viewModelScope.launch {
            val reqId = _activeRequestId.value
            if (reqId != null && requestRepository != null) {
                val existing = requestRepository.getRequestById(reqId)
                val now = nowMs()
                val updatedRequest = existing?.copy(
                    method = parseHttpMethod(_method.value),
                    url = _url.value,
                    headers = _headers.value,
                    queryParams = _queryParams.value,
                    body = _body.value,
                    preRequestScript = _preRequestScript.value.takeIf { it.isNotBlank() },
                    testScript = _testScript.value.takeIf { it.isNotBlank() },
                    updatedAt = now
                ) ?: Request(
                    id = reqId,
                    collectionId = "default",
                    folderId = null,
                    name = _url.value.ifBlank { "Request" },
                    method = parseHttpMethod(_method.value),
                    url = _url.value,
                    headers = _headers.value,
                    queryParams = _queryParams.value,
                    body = _body.value,
                    preRequestScript = _preRequestScript.value.takeIf { it.isNotBlank() },
                    testScript = _testScript.value.takeIf { it.isNotBlank() },
                    createdAt = now,
                    updatedAt = now
                )
                requestRepository.insertRequest(updatedRequest)
                saveDraft()
                onSaved(updatedRequest)
            } else {
                saveDraft()
            }
        }
    }

    fun saveToCollection(
        collectionId: String,
        newCollectionName: String? = null,
        requestName: String? = null,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val targetCollectionId = if (!newCollectionName.isNullOrBlank()) {
                val now = nowMs()
                val col = com.learn.reqlite.domain.model.Collection(
                    id = "col_${now}_${(1000..9999).random()}",
                    name = newCollectionName.trim(),
                    createdAt = now,
                    updatedAt = now
                )
                collectionRepository?.insertCollection(col)
                col.id
            } else {
                collectionId
            }

            val reqName = requestName?.trim()?.takeIf { it.isNotBlank() } ?: _url.value.ifBlank { "Untitled Request" }
            val now = nowMs()
            val request = Request(
                id = "req_${now}_${(1000..9999).random()}",
                collectionId = targetCollectionId,
                folderId = null,
                name = reqName,
                method = parseHttpMethod(_method.value),
                url = _url.value,
                headers = _headers.value,
                queryParams = _queryParams.value,
                body = _body.value,
                preRequestScript = _preRequestScript.value.takeIf { it.isNotBlank() },
                testScript = _testScript.value.takeIf { it.isNotBlank() },
                createdAt = now,
                updatedAt = now
            )

            requestRepository?.insertRequest(request)
            _activeRequestId.value = request.id
            saveDraft()
            onSaved()
        }
    }

    fun setGraphQLSchema(schema: GraphQLSchema?) {
        _graphQLSchema.value = schema
    }

    fun fetchGraphQLSchema() {
        val client = httpClient ?: return
        val currentUrl = _url.value
        if (currentUrl.isBlank()) return

        viewModelScope.launch {
            _isLoadingSchema.value = true
            try {
                val envVariables = _activeEnvironment.value?.id?.let { envId ->
                    environmentRepository?.getEnvironmentById(envId)?.variables
                } ?: emptyList()

                val resolvedVariables = envVariables.map { variable ->
                    if (variable.isSecret && secureStorage != null) {
                        val secretVal = secureStorage.getSecret("env_var_${variable.id}")
                        variable.copy(value = secretVal ?: "")
                    } else {
                        variable
                    }
                }

                val targetUrl = variableResolver.resolve(currentUrl, resolvedVariables)

                val payload = buildJsonObject {
                    put("query", GraphQLIntrospectionParser.INTROSPECTION_QUERY)
                }.toString()

                val statement = client.request(targetUrl) {
                    this.method = KtorHttpMethod.Post
                    header(HttpHeaders.ContentType, "application/json")
                    _headers.value.filter { it.isEnabled }.forEach { h ->
                        header(
                            variableResolver.resolve(h.key, resolvedVariables),
                            variableResolver.resolve(h.value, resolvedVariables)
                        )
                    }
                    setBody(payload)
                }

                val responseText = statement.bodyAsText()
                val parsedSchema = GraphQLIntrospectionParser.parse(responseText)
                _graphQLSchema.value = parsedSchema
            } catch (_: Exception) {
                // Introspection failure leaves existing schema intact
            } finally {
                _isLoadingSchema.value = false
            }
        }
    }
}
