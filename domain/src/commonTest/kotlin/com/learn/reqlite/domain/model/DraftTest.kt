package com.learn.reqlite.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class DraftTest {
    @Test
    fun testDraftCreation() {
        val draft = Draft(
            id = "d1",
            requestId = "r1",
            method = HttpMethod.POST,
            url = "https://api.example.com",
            body = RequestBody.TextBody("{}", "application/json"),
            updatedAt = 2000L
        )

        assertEquals("d1", draft.id)
        assertEquals(HttpMethod.POST, draft.method)
        assertEquals("application/json", (draft.body as RequestBody.TextBody).contentType)
    }
}
