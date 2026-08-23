package com.learn.reqlite.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "request_bodies",
    foreignKeys = [
        ForeignKey(
            entity = RequestEntity::class,
            parentColumns = ["id"],
            childColumns = ["requestId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DraftEntity::class,
            parentColumns = ["id"],
            childColumns = ["draftId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("requestId", unique = true),
        Index("draftId", unique = true)
    ]
)
data class RequestBodyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val requestId: String?,
    val draftId: String?,
    val type: String, // NO_BODY, TEXT, FORM_DATA, URL_ENCODED
    val content: String?,
    val contentType: String?
)
