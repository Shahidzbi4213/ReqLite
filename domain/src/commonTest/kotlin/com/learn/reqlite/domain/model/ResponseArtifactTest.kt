package com.learn.reqlite.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResponseArtifactTest {
    @Test
    fun testResponseArtifactCreation() {
        val artifact = ResponseArtifact(
            id = "a1",
            filePath = "/path/to/file.json",
            contentType = "application/json",
            sizeBytes = 1024L,
            timestamp = 5000L
        )

        assertEquals("a1", artifact.id)
        assertEquals("/path/to/file.json", artifact.filePath)
        assertEquals("application/json", artifact.contentType)
        assertEquals(1024L, artifact.sizeBytes)
    }

    @Test
    fun testResponseArtifact_OptionalContentType() {
        val artifact = ResponseArtifact(
            id = "a2",
            filePath = "/path/to/unknown.bin",
            sizeBytes = 2048L,
            timestamp = 6000L
        )
        assertNull(artifact.contentType)
    }
}
