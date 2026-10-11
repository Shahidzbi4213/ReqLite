package com.learn.reqlite.domain.model

import kotlin.test.Test
import kotlin.test.assertTrue

class RequestBodyTest {
    @Test
    fun testRequestBodyTypes() {
        val noBody: RequestBody = RequestBody.NoBody
        val textBody: RequestBody = RequestBody.TextBody("test", "text/plain")
        val gqlBody: RequestBody = RequestBody.GraphQLBody("{ users { id } }", "{\"limit\": 10}")
        
        assertTrue(noBody is RequestBody.NoBody)
        assertTrue(textBody is RequestBody.TextBody)
        assertTrue(gqlBody is RequestBody.GraphQLBody)
    }
}
