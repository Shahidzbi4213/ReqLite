package com.learn.reqlite.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RequestTest {
    @Test
    fun testRequestCreation() {
        val field = RequestField(id = "f1", key = "Auth", value = "Bearer token")
        val request = Request(
            id = "r1",
            collectionId = "c1",
            name = "Get User",
            method = HttpMethod.GET,
            url = "https://api.example.com/user",
            headers = listOf(field),
            createdAt = 1000L,
            updatedAt = 1000L
        )

        assertEquals("r1", request.id)
        assertEquals(HttpMethod.GET, request.method)
        assertEquals(1, request.headers.size)
        assertTrue(request.headers.first().isEnabled)
        assertEquals(RequestBody.NoBody, request.body)
    }
}
