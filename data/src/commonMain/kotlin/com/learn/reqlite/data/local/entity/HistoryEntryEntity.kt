package com.learn.reqlite.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "history_entries",
    foreignKeys = [
        ForeignKey(
            entity = RequestEntity::class,
            parentColumns = ["id"],
            childColumns = ["requestId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ResponseArtifactEntity::class,
            parentColumns = ["id"],
            childColumns = ["responseArtifactId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("requestId"),
        Index("responseArtifactId")
    ]
)
data class HistoryEntryEntity(
    @PrimaryKey
    val id: String,
    val requestId: String? = null,
    val requestMethod: String,
    val requestUrl: String,
    val statusCode: Int?,
    val durationMs: Long?,
    val timestamp: Long,
    val responseArtifactId: String?,
    val errorCode: String? = null,
    val errorMessage: String? = null
)
