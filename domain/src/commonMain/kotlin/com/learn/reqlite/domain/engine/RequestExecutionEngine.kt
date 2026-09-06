package com.learn.reqlite.domain.engine

import com.learn.reqlite.domain.model.HistoryEntry

interface RequestExecutionEngine {
    suspend fun execute(draftId: String, environmentId: String?): Pair<HistoryEntry, String>
}
