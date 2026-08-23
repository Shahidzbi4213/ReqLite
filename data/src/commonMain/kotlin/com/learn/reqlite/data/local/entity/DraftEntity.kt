package com.learn.reqlite.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "drafts",
    foreignKeys = [
        ForeignKey(
            entity = RequestEntity::class,
            parentColumns = ["id"],
            childColumns = ["requestId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("requestId", unique = true)
    ]
)
data class DraftEntity(
    @PrimaryKey
    val id: String,
    val requestId: String,
    val method: String,
    val url: String,
    val updatedAt: Long
)
