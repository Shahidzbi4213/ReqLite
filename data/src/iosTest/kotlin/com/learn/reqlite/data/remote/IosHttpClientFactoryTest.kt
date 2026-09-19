package com.learn.reqlite.data.remote

import kotlin.test.Test
import kotlin.test.assertNotNull

class IosHttpClientFactoryTest {

    @Test
    fun createHttpClient_instantiatesValidDarwinClient() {
        val client = createHttpClient(enableLogging = false)
        assertNotNull(client)
        client.close()
    }
}
