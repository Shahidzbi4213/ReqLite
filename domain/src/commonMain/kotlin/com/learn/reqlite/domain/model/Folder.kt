package com.learn.reqlite.domain.model

data class Folder(
    val id: String,
    val collectionId: String,
    val parentFolderId: String? = null,
    val name: String,
    val description: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
