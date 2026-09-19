package com.learn.reqlite.data.mapper

import com.learn.reqlite.data.local.entity.CollectionEntity
import com.learn.reqlite.domain.model.Collection
import kotlin.test.Test
import kotlin.test.assertEquals

class CollectionMapperTest {
    @Test
    fun testCollectionToDomain() {
        val entity = CollectionEntity(
            id = "c1",
            name = "Test",
            description = "Desc",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val domain = entity.toDomain()
        assertEquals("c1", domain.id)
        assertEquals("Test", domain.name)
        assertEquals("Desc", domain.description)
    }

    @Test
    fun testCollectionToEntity() {
        val domain = Collection(
            id = "c2",
            name = "Test 2",
            description = null,
            createdAt = 3000L,
            updatedAt = 4000L
        )

        val entity = domain.toEntity()
        assertEquals("c2", entity.id)
        assertEquals(null, entity.description)
    }
}
