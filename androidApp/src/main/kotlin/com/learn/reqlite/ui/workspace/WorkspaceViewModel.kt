package com.learn.reqlite.ui.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learn.reqlite.domain.engine.RequestExecutionEngine
import com.learn.reqlite.domain.model.*
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    private val httpClient: HttpClient? = null,
    private val secureStorage: SecureStorage? = null,
    private val variableResolver: VariableResolver = VariableResolver()
) : ViewModel() {

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

    private var activeExecutionJob: Job? = null

    init {
        loadEnvironments()
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
    }

    fun setMethod(newMethod: String) {
        _method.value = newMethod
    }

    fun setQueryParams(newParams: List<RequestField>) {
        _queryParams.value = newParams
    }

    fun setHeaders(newHeaders: List<RequestField>) {
        _headers.value = newHeaders
    }

    fun setBody(newBody: RequestBody) {
        _body.value = newBody
    }

    fun setAuthConfiguration(newAuth: AuthConfiguration) {
        _authConfiguration.value = newAuth
    }

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

            val resolvedUrl = variableResolver.resolve(currentUrl, resolvedVariables)
            val client = httpClient ?: HttpClient()
            val startTime = nowMs()

            try {
                val statement = client.request(resolvedUrl) {
                    this.method = KtorHttpMethod.parse(_method.value)

                    // Inject Headers
                    _headers.value.filter { it.isEnabled }.forEach { header ->
                        val k = variableResolver.resolve(header.key, resolvedVariables)
                        val v = variableResolver.resolve(header.value, resolvedVariables)
                        if (k.isNotBlank()) {
                            header(k, v)
                        }
                    }

                    // Inject Auth
                    when (val auth = _authConfiguration.value) {
                        is AuthConfiguration.None -> {}
                        is AuthConfiguration.Bearer -> {
                            val resolvedToken = variableResolver.resolve(auth.token, resolvedVariables)
                            header(HttpHeaders.Authorization, "Bearer $resolvedToken")
                        }
                        is AuthConfiguration.Basic -> {
                            val u = variableResolver.resolve(auth.username, resolvedVariables)
                            val p = variableResolver.resolve(auth.password, resolvedVariables)
                            val creds = "$u:$p".encodeBase64()
                            header(HttpHeaders.Authorization, "Basic $creds")
                        }
                    }

                    // Inject Query Params
                    _queryParams.value.filter { it.isEnabled }.forEach { qp ->
                        val k = variableResolver.resolve(qp.key, resolvedVariables)
                        val v = variableResolver.resolve(qp.value, resolvedVariables)
                        if (k.isNotBlank()) {
                            parameter(k, v)
                        }
                    }

                    // Inject Body
                    when (val reqBody = _body.value) {
                        is RequestBody.NoBody -> {}
                        is RequestBody.TextBody -> {
                            val resolvedContent = variableResolver.resolve(reqBody.content, resolvedVariables)
                            val resolvedContentType = variableResolver.resolve(reqBody.contentType, resolvedVariables)
                            header(HttpHeaders.ContentType, resolvedContentType.ifBlank { "application/json" })
                            setBody(resolvedContent)
                        }
                        is RequestBody.FormDataBody -> {}
                        is RequestBody.UrlEncodedBody -> {
                            val pairs = reqBody.fields.filter { it.isEnabled }.map {
                                val k = variableResolver.resolve(it.key, resolvedVariables)
                                val v = variableResolver.resolve(it.value, resolvedVariables)
                                "$k=$v"
                            }.joinToString("&")
                            header(HttpHeaders.ContentType, "application/x-www-form-urlencoded")
                            setBody(pairs)
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
                    artifactId = null
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
                    val (entry, _) = executionEngine.execute(draftId, environmentId)
                    val uiModel = HttpResponseUiModel(
                        statusCode = entry.statusCode ?: 200,
                        statusText = "OK",
                        durationMs = entry.durationMs ?: 0L,
                        sizeBytes = 0L,
                        contentType = "application/json",
                        headers = emptyList(),
                        body = "",
                        timestamp = entry.timestamp,
                        url = entry.requestUrl,
                        method = entry.requestMethod.name,
                        artifactId = entry.responseArtifactId
                    )
                    _executionState.value = ExecutionUiState.Success(
                        historyEntry = entry,
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

    fun saveDraft(draftId: String = "draft_${nowMs()}") {
        viewModelScope.launch {
            val draft = Draft(
                id = draftId,
                requestId = null,
                method = parseHttpMethod(_method.value),
                url = _url.value,
                headers = _headers.value,
                queryParams = _queryParams.value,
                body = _body.value,
                updatedAt = nowMs()
            )
            requestRepository?.insertDraft(draft)
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
        }
    }

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
}
