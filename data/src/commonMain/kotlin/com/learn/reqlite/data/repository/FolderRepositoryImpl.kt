package com.learn.reqlite.data.repository

import com.learn.reqlite.data.local.dao.FolderDao
import com.learn.reqlite.data.mapper.toDomain
import com.learn.reqlite.data.mapper.toEntity
import com.learn.reqlite.domain.model.Folder
import com.learn.reqlite.domain.repository.FolderRepository

class FolderRepositoryImpl(
    private val folderDao: FolderDao
) : FolderRepository {

    override suspend fun insertFolder(folder: Folder) {
        folderDao.insertFolder(folder.toEntity())
    }

    override suspend fun updateFolder(folder: Folder) {
        folderDao.updateFolder(folder.toEntity())
    }

    override suspend fun deleteFolder(folder: Folder) {
        folderDao.deleteFolder(folder.toEntity())
    }

    override suspend fun getFoldersByCollectionId(collectionId: String): List<Folder> {
        return folderDao.getFoldersByCollectionId(collectionId).map { it.toDomain() }
    }

    override suspend fun getFolderById(id: String): Folder? {
        return folderDao.getFolderById(id)?.toDomain()
    }
}
