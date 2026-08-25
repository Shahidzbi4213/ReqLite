package com.learn.reqlite.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.learn.reqlite.data.local.entity.DraftEntity
import com.learn.reqlite.data.local.entity.RequestBodyEntity
import com.learn.reqlite.data.local.entity.RequestEntity
import com.learn.reqlite.data.local.entity.RequestFieldEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RequestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: RequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequestFields(fields: List<RequestFieldEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequestBody(body: RequestBodyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: DraftEntity)

    @Query("SELECT * FROM requests WHERE id = :id")
    suspend fun getRequestById(id: String): RequestEntity?

    @Query("SELECT * FROM request_fields WHERE requestId = :requestId")
    suspend fun getFieldsForRequest(requestId: String): List<RequestFieldEntity>

    @Query("SELECT * FROM request_bodies WHERE requestId = :requestId")
    suspend fun getBodyForRequest(requestId: String): RequestBodyEntity?

    @Query("SELECT * FROM drafts WHERE requestId = :requestId")
    suspend fun getDraftForRequest(requestId: String): DraftEntity?

    @Query("SELECT * FROM drafts WHERE id = :id")
    suspend fun getDraftById(id: String): DraftEntity?

    @Query("SELECT * FROM request_fields WHERE draftId = :draftId")
    suspend fun getFieldsForDraft(draftId: String): List<RequestFieldEntity>

    @Query("SELECT * FROM request_bodies WHERE draftId = :draftId")
    suspend fun getBodyForDraft(draftId: String): RequestBodyEntity?

    @Query("SELECT * FROM requests")
    fun getAllRequests(): Flow<List<RequestEntity>>

    @Query("DELETE FROM requests WHERE id = :id")
    suspend fun deleteRequest(id: String)
}
