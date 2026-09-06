package com.learn.reqlite.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HistoryEntryTest {
    @Test
    fun testHistoryEntryCreation() {
        val entry = HistoryEntry(
            id = "h1",
            requestId = "r1",
            requestMethod = HttpMethod.GET,
            requestUrl = "https://example.com",
            statusCode = 200,
            durationMs = 120L,
            timestamp = 1000L,
            responseArtifactId = "a1"
        )

        assertEquals("h1", entry.id)
        assertEquals("r1", entry.requestId)
        assertEquals(HttpMethod.GET, entry.requestMethod)
        assertEquals("https://example.com", entry.requestUrl)
        assertEquals(200, entry.statusCode)
        assertEquals(120L, entry.durationMs)
        assertEquals("a1", entry.responseArtifactId)
    }

    @Test
    fun testHistoryEntry_FailedRequest() {
        val entry = HistoryEntry(
            id = "h2",
            requestId = "r2",
            requestMethod = HttpMethod.POST,
            requestUrl = "https://example.com",
            timestamp = 2000L
        )

        assertNull(entry.statusCode)
        assertNull(entry.durationMs)
        assertNull(entry.responseArtifactId)
    }
}
