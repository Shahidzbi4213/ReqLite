package com.learn.reqlite.data.local.storage

import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IosKeychainSecureStorageTest {

    private lateinit var storage: IosKeychainSecureStorage

    @BeforeTest
    fun setup() {
        storage = IosKeychainSecureStorage(serviceName = "com.learn.reqlite.test.keychain")
        storage.clearAllSecrets()
    }

    @AfterTest
    fun tearDown() {
        storage.clearAllSecrets()
    }

    @Test
    fun saveSecret_and_getSecret_returnsCorrectValue() = runTest {
        storage.saveSecret("jwt_token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9")

        val retrieved = storage.getSecret("jwt_token")
        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9", retrieved)
    }

    @Test
    fun saveSecret_updatesExistingSecret() = runTest {
        storage.saveSecret("api_key", "old_value_123")
        assertEquals("old_value_123", storage.getSecret("api_key"))

        // Update with new value
        storage.saveSecret("api_key", "new_value_456")
        assertEquals("new_value_456", storage.getSecret("api_key"))
    }

    @Test
    fun getSecret_nonExistentKey_returnsNull() = runTest {
        val result = storage.getSecret("unknown_key_999")
        assertNull(result)
    }

    @Test
    fun deleteSecret_removesItem() = runTest {
        storage.saveSecret("secret_to_delete", "temporary_val")
        assertEquals("temporary_val", storage.getSecret("secret_to_delete"))

        storage.deleteSecret("secret_to_delete")
        assertNull(storage.getSecret("secret_to_delete"))
    }

    @Test
    fun deleteSecret_nonExistentKey_doesNotThrow() = runTest {
        storage.deleteSecret("non_existent_key_abc")
        assertNull(storage.getSecret("non_existent_key_abc"))
    }

    @Test
    fun multipleKeys_isolation() = runTest {
        storage.saveSecret("user_a", "token_a")
        storage.saveSecret("user_b", "token_b")
        storage.saveSecret("user_c", "token_c")

        assertEquals("token_a", storage.getSecret("user_a"))
        assertEquals("token_b", storage.getSecret("user_b"))
        assertEquals("token_c", storage.getSecret("user_c"))

        storage.deleteSecret("user_b")

        assertEquals("token_a", storage.getSecret("user_a"))
        assertNull(storage.getSecret("user_b"))
        assertEquals("token_c", storage.getSecret("user_c"))
    }

    @Test
    fun complexPayloads_supportsSpecialCharactersAndJson() = runTest {
        val complexString = """
            {
                "token": "secret_!@#$%^&*()_+{}[]:\"|;'<>?,./",
                "emoji": "🔑🔐🚀✨",
                "multiline": "line1\nline2\r\nline3"
            }
        """.trimIndent()

        storage.saveSecret("complex_secret", complexString)
        val retrieved = storage.getSecret("complex_secret")
        assertEquals(complexString, retrieved)
    }

    @Test
    fun clearAllSecrets_removesAllEntries() = runTest {
        storage.saveSecret("key1", "val1")
        storage.saveSecret("key2", "val2")

        storage.clearAllSecrets()

        assertNull(storage.getSecret("key1"))
        assertNull(storage.getSecret("key2"))
    }
}
