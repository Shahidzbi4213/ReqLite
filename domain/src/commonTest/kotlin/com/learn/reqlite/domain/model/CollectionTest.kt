package com.learn.reqlite.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CollectionTest {

    @Test
    fun testCollectionCreation() {
        val collection = Collection(
            id = "c1",
            name = "My Collection",
            description = "Test Description",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        assertEquals("c1", collection.id)
        assertEquals("My Collection", collection.name)
        assertEquals("Test Description", collection.description)
        assertEquals(1000L, collection.createdAt)
        assertEquals(2000L, collection.updatedAt)
    }

    @Test
    fun testCollectionCreation_OptionalDescription() {
        val collection = Collection(
            id = "c2",
            name = "Empty Desc",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        assertNull(collection.description)
    }
}
