package com.learn.reqlite.data.local.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AndroidSecureStorageTest {

    private lateinit var secureStorage: AndroidSecureStorage

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        secureStorage = AndroidSecureStorage(context)
    }

    @Test
    fun testSaveAndGetSecret() {
        val key = "test_key"
        val value = "super_secret_value"

        secureStorage.saveSecret(key, value)
        
        val retrievedValue = secureStorage.getSecret(key)
        assertEquals(value, retrievedValue)
    }

    @Test
    fun testDeleteSecret() {
        val key = "test_key_delete"
        val value = "to_be_deleted"

        secureStorage.saveSecret(key, value)
        secureStorage.deleteSecret(key)

        val retrievedValue = secureStorage.getSecret(key)
        assertNull(retrievedValue)
    }
}
