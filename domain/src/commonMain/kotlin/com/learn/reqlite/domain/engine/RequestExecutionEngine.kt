package com.learn.reqlite.domain.engine

import com.learn.reqlite.domain.model.HistoryEntry
import com.learn.reqlite.domain.script.model.TestAssertionResult

data class RequestExecutionResult(
    val historyEntry: HistoryEntry,
    val responseBody: String,
    val responseHeaders: Map<String, String> = emptyMap(),
    val testResults: List<TestAssertionResult> = emptyList(),
    val scriptLogs: List<String> = emptyList()
)

interface RequestExecutionEngine {
    suspend fun execute(draftId: String, environmentId: String?): Pair<HistoryEntry, String>

    suspend fun executeWithHeaders(draftId: String, environmentId: String?): RequestExecutionResult {
        val (entry, body) = execute(draftId, environmentId)
        return RequestExecutionResult(entry, body, emptyMap())
    }
}
