package com.learn.reqlite.domain.repository

import com.learn.reqlite.domain.model.Draft
import com.learn.reqlite.domain.model.Request
import kotlinx.coroutines.flow.Flow

interface RequestRepository {
    suspend fun insertRequest(request: Request)
    suspend fun getRequestById(id: String): Request?
    fun getAllRequests(): Flow<List<Request>>
    suspend fun deleteRequest(id: String)

    suspend fun insertDraft(draft: Draft)
    suspend fun getDraftById(id: String): Draft?
    suspend fun getDraftForRequest(requestId: String): Draft?
    fun getAllDrafts(): Flow<List<Draft>>
    suspend fun deleteDraft(id: String)
}
