package com.learn.reqlite.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VariableTest {
    @Test
    fun testVariableCreation() {
        val variable = Variable(id = "v1", key = "KEY", value = "VALUE")
        assertEquals("v1", variable.id)
        assertEquals("KEY", variable.key)
        assertTrue(variable.isEnabled)
    }
}
