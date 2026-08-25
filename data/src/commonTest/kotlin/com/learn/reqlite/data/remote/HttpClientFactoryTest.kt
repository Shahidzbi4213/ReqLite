package com.learn.reqlite.data.remote

import kotlin.test.Test
import kotlin.test.assertNotNull

class HttpClientFactoryTest {

    @Test
    fun testCreateHttpClient() {
        // Create an instance of the HTTP client
        val client = createHttpClient(enableLogging = true)
        
        // Assert it was successfully created
        assertNotNull(client)
        
        // Close it immediately to avoid memory leaks
        client.close()
    }
}
