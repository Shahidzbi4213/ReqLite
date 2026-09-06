package com.learn.reqlite.ui.workspace

import com.learn.reqlite.di.DiHelper
import com.learn.reqlite.domain.engine.RequestExecutionEngine
import com.learn.reqlite.domain.model.*
import com.learn.reqlite.domain.repository.EnvironmentRepository
import com.learn.reqlite.domain.repository.RequestRepository
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import platform.posix.time

sealed class IosExecutionState {
    object Idle : IosExecutionState()
    object Loading : IosExecutionState()
    data class Success(val result: HistoryEntry, val responseBody: String) : IosExecutionState()
    data class Error(val message: String) : IosExecutionState()
}

class IosWorkspaceAdapter(
    private val executionEngine: RequestExecutionEngine,
    private val environmentRepository: EnvironmentRepository,
    private val requestRepository: RequestRepository,
    private val scope: CoroutineScope
) {
    constructor() : this(
        executionEngine = DiHelper.getRequestExecutionEngine(),
        environmentRepository = DiHelper.getEnvironmentRepository(),
        requestRepository = DiHelper.getRequestRepository(),
        scope = CoroutineScope(Dispatchers.Main)
    )

    private var envJob: Job? = null
    private var executionJob: Job? = null

    fun observeEnvironments(onEnvironmentsChanged: (List<Environment>) -> Unit) {
        envJob?.cancel()
        envJob = environmentRepository.getAllEnvironments()
            .onEach { onEnvironmentsChanged(it) }
            .launchIn(scope)
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
                val resultPair = executionEngine.execute(draftId, environmentId)
                onStateChanged(IosExecutionState.Success(resultPair.first, resultPair.second))
            } catch (e: Exception) {
                onStateChanged(IosExecutionState.Error(e.message ?: "Execution failed"))
            }
        }
    }

    fun cancelExecution(onStateChanged: (IosExecutionState) -> Unit) {
        executionJob?.cancel()
        onStateChanged(IosExecutionState.Idle)
    }

    @OptIn(ExperimentalForeignApi::class)
    fun createQuickDraft(
        methodName: String,
        url: String,
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
            val draft = Draft(
                id = "ios_draft_${time(null)}",
                requestId = null,
                method = method,
                url = url,
                headers = emptyList(),
                queryParams = emptyList(),
                body = RequestBody.NoBody,
                updatedAt = 1000L
            )
            requestRepository.insertDraft(draft)
            onCreated(draft.id)
        }
    }

    fun close() {
        envJob?.cancel()
        executionJob?.cancel()
    }
}
