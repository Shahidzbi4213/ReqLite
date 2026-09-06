package com.learn.reqlite.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val createdAt: Long,
    val updatedAt: Long
)
