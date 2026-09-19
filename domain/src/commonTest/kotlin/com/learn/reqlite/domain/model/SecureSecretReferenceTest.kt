package com.learn.reqlite.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class SecureSecretReferenceTest {
    @Test
    fun testSecureSecretReferenceCreation() {
        val ref = SecureSecretReference(id = "ref1", key = "API_KEY", keystoreId = "ks_12345")
        assertEquals("ref1", ref.id)
        assertEquals("API_KEY", ref.key)
        assertEquals("ks_12345", ref.keystoreId)
    }
}
