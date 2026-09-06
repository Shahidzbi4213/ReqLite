package com.learn.reqlite.ui.response

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learn.reqlite.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ResponseViewModel(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _response = MutableStateFlow<HttpResponseUiModel?>(null)
    val response: StateFlow<HttpResponseUiModel?> = _response.asStateFlow()

    fun loadResponse(historyId: String) {
        viewModelScope.launch {
            val entry = historyRepository.getHistoryEntryById(historyId)
            if (entry != null) {
                _response.value = HttpResponseUiModel(
                    statusCode = entry.statusCode,
                    durationMs = entry.durationMs,
                    url = entry.requestUrl,
                    method = entry.requestMethod.name,
                    artifactId = entry.responseArtifactId,
                    error = entry.errorMessage
                )
            }
        }
    }
}
