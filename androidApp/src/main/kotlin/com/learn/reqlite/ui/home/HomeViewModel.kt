package com.learn.reqlite.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learn.reqlite.domain.model.Collection
import com.learn.reqlite.domain.model.Draft
import com.learn.reqlite.domain.model.HistoryEntry
import com.learn.reqlite.domain.model.Request
import com.learn.reqlite.domain.repository.CollectionRepository
import com.learn.reqlite.domain.repository.HistoryRepository
import com.learn.reqlite.domain.repository.RequestRepository
import com.learn.reqlite.utils.common.nowMs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.learn.reqlite.domain.export.WorkspaceImporter
import com.learn.reqlite.domain.export.WorkspaceImportResult
import com.learn.reqlite.domain.parser.PostmanCollectionParser
import com.learn.reqlite.domain.parser.PostmanCollectionParserImpl
import com.learn.reqlite.domain.parser.PostmanParseResult

class HomeViewModel(
    private val historyRepository: HistoryRepository,
    private val requestRepository: RequestRepository,
    private val collectionRepository: CollectionRepository? = null,
    private val workspaceImporter: WorkspaceImporter? = null,
    private val postmanParser: PostmanCollectionParser = PostmanCollectionParserImpl()
) : ViewModel() {

    private val effectiveImporter: WorkspaceImporter? by lazy {
        workspaceImporter ?: collectionRepository?.let { colRepo ->
            com.learn.reqlite.domain.export.WorkspaceExportImportManager(
                collectionRepository = colRepo,
                requestRepository = requestRepository,
                environmentRepository = object : com.learn.reqlite.domain.repository.EnvironmentRepository {
                    override fun getAllEnvironments() = kotlinx.coroutines.flow.flowOf(emptyList<com.learn.reqlite.domain.model.Environment>())
                    override suspend fun getEnvironmentById(id: String) = null
                    override suspend fun insertEnvironment(environment: com.learn.reqlite.domain.model.Environment) {}
                    override suspend fun updateEnvironment(environment: com.learn.reqlite.domain.model.Environment) {}
                    override suspend fun deleteEnvironment(id: String) {}
                },
                postmanParser = postmanParser
            )
        }
    }

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

    val collections: StateFlow<List<Collection>> = (collectionRepository?.getAllCollections() ?: emptyFlow())
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

    fun deleteRequest(id: String) {
        viewModelScope.launch {
            requestRepository.deleteRequest(id)
        }
    }

    fun createCollection(name: String, description: String? = null, onCreated: ((Collection) -> Unit)? = null) {
        viewModelScope.launch {
            val now = nowMs()
            val col = Collection(
                id = "col_${now}_${(1000..9999).random()}",
                name = name.trim(),
                description = description?.trim(),
                createdAt = now,
                updatedAt = now
            )
            collectionRepository?.insertCollection(col)
            onCreated?.invoke(col)
        }
    }

    fun deleteCollection(collection: Collection) {
        viewModelScope.launch {
            collectionRepository?.deleteCollection(collection)
        }
    }

    fun openSavedRequest(request: Request, onComplete: (draftId: String) -> Unit) {
        viewModelScope.launch {
            val draft = Draft(
                id = "draft_${nowMs()}",
                requestId = request.id,
                method = request.method,
                url = request.url,
                headers = request.headers,
                queryParams = request.queryParams,
                body = request.body,
                updatedAt = nowMs()
            )
            requestRepository.insertDraft(draft)
            onComplete(draft.id)
        }
    }

    fun importCurl(curlCommand: String, onComplete: (draftId: String) -> Unit) {
        viewModelScope.launch {
            val parser = com.learn.reqlite.domain.parser.CurlParserImpl()
            val result = parser.parse(curlCommand)
            if (result is com.learn.reqlite.domain.parser.CurlParseResult.Success) {
                requestRepository.insertDraft(result.draft)
                onComplete(result.draft.id)
            }
        }
    }

    fun importPostmanCollection(
        jsonContent: String,
        onComplete: (Result<Int>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val importer = effectiveImporter
                if (importer != null) {
                    when (val res = importer.importPostmanCollection(jsonContent)) {
                        is WorkspaceImportResult.Success -> onComplete(Result.success(res.requestsImported))
                        is WorkspaceImportResult.Error -> onComplete(Result.failure(Exception(res.message)))
                    }
                } else {
                    onComplete(Result.failure(Exception("Workspace importer not available")))
                }
            } catch (e: Exception) {
                onComplete(Result.failure(e))
            }
        }
    }
}
