package com.learn.reqlite.data.mapper

import com.learn.reqlite.data.local.entity.FolderEntity
import com.learn.reqlite.domain.model.Folder

fun FolderEntity.toDomain(): Folder {
    return Folder(
        id = id,
        collectionId = collectionId,
        parentFolderId = parentFolderId,
        name = name,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Folder.toEntity(): FolderEntity {
    return FolderEntity(
        id = id,
        collectionId = collectionId,
        parentFolderId = parentFolderId,
        name = name,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
