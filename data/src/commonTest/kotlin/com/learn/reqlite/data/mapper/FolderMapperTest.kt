package com.learn.reqlite.data.mapper

import com.learn.reqlite.data.local.entity.FolderEntity
import com.learn.reqlite.domain.model.Folder
import kotlin.test.Test
import kotlin.test.assertEquals

class FolderMapperTest {
    @Test
    fun testFolderToDomain() {
        val entity = FolderEntity(
            id = "f1",
            collectionId = "c1",
            parentFolderId = null,
            name = "My Folder",
            description = "Desc",
            createdAt = 100L,
            updatedAt = 200L
        )

        val domain = entity.toDomain()
        assertEquals("f1", domain.id)
        assertEquals("c1", domain.collectionId)
        assertEquals(null, domain.parentFolderId)
        assertEquals("My Folder", domain.name)
    }
}
