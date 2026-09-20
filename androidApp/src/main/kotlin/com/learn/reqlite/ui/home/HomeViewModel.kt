package com.learn.reqlite.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learn.reqlite.domain.model.Draft
import com.learn.reqlite.domain.model.HistoryEntry
import com.learn.reqlite.domain.model.Request
import com.learn.reqlite.domain.repository.HistoryRepository
import com.learn.reqlite.domain.repository.RequestRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val historyRepository: HistoryRepository,
    private val requestRepository: RequestRepository
) : ViewModel() {

    val recentRequests: StateFlow<List<HistoryEntry>> = historyRepository.getAllHistoryEntries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val drafts: StateFlow<List<Draft>> = requestRepository.getAllDrafts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val savedRequests: StateFlow<List<Request>> = requestRepository.getAllRequests()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }

    fun deleteDraft(id: String) {
        viewModelScope.launch {
            requestRepository.deleteDraft(id)
        }
    }

    fun importCurl(curlCommand: String, onComplete: (draftId: String) -> Unit) {
        viewModelScope.launch {
            val parser = com.learn.reqlite.domain.parser.CurlParserImpl()
            val result = parser.parse(curlCommand)
            if (result is com.learn.reqlite.domain.parser.CurlParseResult.Success) {
                requestRepository.insertDraft(result.draft)
                onComplete(result.draft.id)
            } else {
                val fallbackDraft = Draft(
                    id = "draft_${com.learn.reqlite.utils.common.nowMs()}",
                    url = curlCommand.trim(),
                    method = com.learn.reqlite.domain.model.HttpMethod.GET,
                    updatedAt = com.learn.reqlite.utils.common.nowMs()
                )
                requestRepository.insertDraft(fallbackDraft)
                onComplete(fallbackDraft.id)
            }
        }
    }
}
