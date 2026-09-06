package com.learn.reqlite.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FolderTest {

    @Test
    fun testFolderCreation() {
        val folder = Folder(
            id = "f1",
            collectionId = "c1",
            parentFolderId = "f0",
            name = "My Folder",
            description = "Test Desc",
            createdAt = 1500L,
            updatedAt = 2500L
        )

        assertEquals("f1", folder.id)
        assertEquals("c1", folder.collectionId)
        assertEquals("f0", folder.parentFolderId)
        assertEquals("My Folder", folder.name)
        assertEquals("Test Desc", folder.description)
        assertEquals(1500L, folder.createdAt)
        assertEquals(2500L, folder.updatedAt)
    }

    @Test
    fun testFolderCreation_OptionalFields() {
        val folder = Folder(
            id = "f2",
            collectionId = "c2",
            name = "Empty Folder",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        assertNull(folder.parentFolderId)
        assertNull(folder.description)
    }
}
