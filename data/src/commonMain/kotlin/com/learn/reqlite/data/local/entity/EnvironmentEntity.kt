package com.learn.reqlite.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "environments")
data class EnvironmentEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val color: String?,
    val createdAt: Long,
    val updatedAt: Long
)
