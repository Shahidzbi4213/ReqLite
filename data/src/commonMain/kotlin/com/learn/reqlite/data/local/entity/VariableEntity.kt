package com.learn.reqlite.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "variables",
    foreignKeys = [
        ForeignKey(
            entity = EnvironmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["environmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("environmentId")
    ]
)
data class VariableEntity(
    @PrimaryKey
    val id: String,
    val environmentId: String,
    val key: String,
    val value: String,
    val isEnabled: Boolean,
    val isSecret: Boolean
)
