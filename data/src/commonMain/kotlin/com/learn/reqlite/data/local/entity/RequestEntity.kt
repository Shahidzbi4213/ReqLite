package com.learn.reqlite.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "requests",
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("collectionId"),
        Index("folderId")
    ]
)
data class RequestEntity(
    @PrimaryKey
    val id: String,
    val collectionId: String,
    val folderId: String?,
    val name: String,
    val method: String,
    val url: String,
    val createdAt: Long,
    val updatedAt: Long
)
