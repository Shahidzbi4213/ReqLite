package com.learn.reqlite.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "request_fields",
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
        Index("requestId"),
        Index("draftId")
    ]
)
data class RequestFieldEntity(
    @PrimaryKey
    val id: String,
    val requestId: String?,
    val draftId: String?,
    val type: String, // HEADER, QUERY_PARAM, FORM_DATA, URL_ENCODED
    val key: String,
    val value: String,
    val isEnabled: Boolean,
    val description: String?
)
