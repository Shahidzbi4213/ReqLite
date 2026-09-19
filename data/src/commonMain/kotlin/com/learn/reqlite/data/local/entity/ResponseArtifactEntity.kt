package com.learn.reqlite.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "response_artifacts"
)
data class ResponseArtifactEntity(
    @PrimaryKey
    val id: String,
    val filePath: String,
    val contentType: String?,
    val sizeBytes: Long,
    val timestamp: Long
)
