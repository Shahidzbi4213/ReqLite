package com.learn.reqlite.data.repository

import com.learn.reqlite.data.local.dao.CollectionDao
import com.learn.reqlite.data.mapper.toDomain
import com.learn.reqlite.data.mapper.toEntity
import com.learn.reqlite.domain.model.Collection
import com.learn.reqlite.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CollectionRepositoryImpl(
    private val collectionDao: CollectionDao
) : CollectionRepository {

    override suspend fun insertCollection(collection: Collection) {
        collectionDao.insertCollection(collection.toEntity())
    }

    override suspend fun updateCollection(collection: Collection) {
        collectionDao.updateCollection(collection.toEntity())
    }

    override suspend fun deleteCollection(collection: Collection) {
        collectionDao.deleteCollection(collection.toEntity())
    }

    override fun getAllCollections(): Flow<List<Collection>> {
        return collectionDao.getAllCollections().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCollectionById(id: String): Collection? {
        return collectionDao.getCollectionById(id)?.toDomain()
    }
}
