package com.learn.reqlite.ui.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.learn.reqlite.domain.model.HistoryEntry

sealed interface ExecutionUiState {
    data object Idle : ExecutionUiState
    data class Loading(val progress: Float? = null) : ExecutionUiState
    data class Streaming(val partialResponse: String) : ExecutionUiState
    data class Success(
        val historyEntry: HistoryEntry,
        val response: com.learn.reqlite.ui.response.HttpResponseUiModel = com.learn.reqlite.ui.response.HttpResponseUiModel(
            statusCode = historyEntry.statusCode,
            durationMs = historyEntry.durationMs,
            url = historyEntry.requestUrl,
            method = historyEntry.requestMethod.name,
            artifactId = historyEntry.responseArtifactId,
            error = historyEntry.errorMessage
        )
    ) : ExecutionUiState
    data class Error(val message: String) : ExecutionUiState
}

@Composable
fun ExecutionStatePanel(
    state: ExecutionUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (state is ExecutionUiState.Success) 0.dp else 16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is ExecutionUiState.Idle -> {
                    Text(
                        text = "Enter a URL and hit Send",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is ExecutionUiState.Loading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (state.progress != null) {
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Loading... ${(state.progress * 100).toInt()}%")
                        } else {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Sending request...")
                        }
                    }
                }
                is ExecutionUiState.Streaming -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Streaming Response...", style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.partialResponse,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Start
                        )
                    }
                }
                is ExecutionUiState.Success -> {
                    com.learn.reqlite.ui.response.ResponseViewer(
                        response = state.response,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is ExecutionUiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
