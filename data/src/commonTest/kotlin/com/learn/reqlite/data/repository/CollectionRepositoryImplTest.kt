package com.learn.reqlite.data.repository

import com.learn.reqlite.data.local.dao.CollectionDao
import com.learn.reqlite.data.local.entity.CollectionEntity
import com.learn.reqlite.domain.model.Collection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first

class FakeCollectionDao : CollectionDao {
    private val collections = mutableMapOf<String, CollectionEntity>()
    private val collectionsFlow = MutableStateFlow<List<CollectionEntity>>(emptyList())

    private fun emitFlow() {
        collectionsFlow.value = collections.values.toList()
    }

    override suspend fun insertCollection(collection: CollectionEntity) {
        collections[collection.id] = collection
        emitFlow()
    }

    override suspend fun updateCollection(collection: CollectionEntity) {
        collections[collection.id] = collection
        emitFlow()
    }

    override suspend fun deleteCollection(collection: CollectionEntity) {
        collections.remove(collection.id)
        emitFlow()
    }

    override fun getAllCollections(): Flow<List<CollectionEntity>> {
        return collectionsFlow
    }

    override suspend fun getCollectionById(id: String): CollectionEntity? {
        return collections[id]
    }
}

class CollectionRepositoryImplTest {

    @Test
    fun testInsertAndGet() = runTest {
        val dao = FakeCollectionDao()
        val repo = CollectionRepositoryImpl(dao)

        val collection = Collection(
            id = "1",
            name = "My Collection",
            description = null,
            createdAt = 100L,
            updatedAt = 100L
        )

        repo.insertCollection(collection)

        val retrieved = repo.getCollectionById("1")
        assertEquals(collection, retrieved)
    }

    @Test
    fun testUpdate() = runTest {
        val dao = FakeCollectionDao()
        val repo = CollectionRepositoryImpl(dao)

        val collection = Collection(
            id = "1",
            name = "My Collection",
            description = null,
            createdAt = 100L,
            updatedAt = 100L
        )
        repo.insertCollection(collection)

        val updated = collection.copy(name = "Updated")
        repo.updateCollection(updated)

        val retrieved = repo.getCollectionById("1")
        assertEquals("Updated", retrieved?.name)
    }

    @Test
    fun testDelete() = runTest {
        val dao = FakeCollectionDao()
        val repo = CollectionRepositoryImpl(dao)

        val collection = Collection(
            id = "1",
            name = "My Collection",
            description = null,
            createdAt = 100L,
            updatedAt = 100L
        )
        repo.insertCollection(collection)
        repo.deleteCollection(collection)

        assertNull(repo.getCollectionById("1"))
    }

    @Test
    fun testGetAll() = runTest {
        val dao = FakeCollectionDao()
        val repo = CollectionRepositoryImpl(dao)

        val collection1 = Collection(
            id = "1",
            name = "My Collection",
            description = null,
            createdAt = 100L,
            updatedAt = 100L
        )
        val collection2 = Collection(
            id = "2",
            name = "My Collection 2",
            description = null,
            createdAt = 100L,
            updatedAt = 100L
        )
        repo.insertCollection(collection1)
        repo.insertCollection(collection2)

        val list = repo.getAllCollections().first()
        assertEquals(2, list.size)
    }
}
