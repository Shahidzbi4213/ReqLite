package com.learn.reqlite.domain.repository

import com.learn.reqlite.domain.model.Collection
import kotlinx.coroutines.flow.Flow

interface CollectionRepository {
    suspend fun insertCollection(collection: Collection)
    suspend fun updateCollection(collection: Collection)
    suspend fun deleteCollection(collection: Collection)
    fun getAllCollections(): Flow<List<Collection>>
    suspend fun getCollectionById(id: String): Collection?
}
