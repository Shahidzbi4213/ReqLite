package com.learn.reqlite.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.learn.reqlite.data.local.entity.HistoryEntryEntity
import com.learn.reqlite.data.local.entity.ResponseArtifactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(entry: HistoryEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResponseArtifact(artifact: ResponseArtifactEntity)

    @Query("SELECT * FROM history_entries ORDER BY timestamp DESC")
    fun getAllHistoryEntries(): Flow<List<HistoryEntryEntity>>

    @Query("SELECT * FROM history_entries WHERE requestId = :requestId ORDER BY timestamp DESC")
    fun getHistoryForRequest(requestId: String): Flow<List<HistoryEntryEntity>>

    @Query("SELECT * FROM response_artifacts WHERE id = :id")
    suspend fun getResponseArtifactById(id: String): ResponseArtifactEntity?

    @Query("DELETE FROM history_entries WHERE id = :id")
    suspend fun deleteHistoryEntry(id: String)

    @Query("DELETE FROM history_entries")
    suspend fun clearHistory()
}
