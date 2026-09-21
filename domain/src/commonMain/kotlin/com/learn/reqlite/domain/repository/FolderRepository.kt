package com.learn.reqlite.domain.repository

import com.learn.reqlite.domain.model.Folder

interface FolderRepository {
    suspend fun insertFolder(folder: Folder)
    suspend fun updateFolder(folder: Folder)
    suspend fun deleteFolder(folder: Folder)
    suspend fun getFoldersByCollectionId(collectionId: String): List<Folder>
    suspend fun getFolderById(id: String): Folder?
}
