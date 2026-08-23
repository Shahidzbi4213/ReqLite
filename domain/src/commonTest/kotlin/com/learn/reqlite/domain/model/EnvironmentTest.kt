package com.learn.reqlite.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class EnvironmentTest {
    @Test
    fun testEnvironmentCreation() {
        val variable = Variable(id = "v1", key = "BASE_URL", value = "https://api.example.com", isSecret = false)
        val secretVar = Variable(id = "v2", key = "TOKEN", value = "secret", isSecret = true)
        
        val env = Environment(
            id = "e1",
            name = "Production",
            color = "#FF0000",
            variables = listOf(variable, secretVar),
            createdAt = 1000L,
            updatedAt = 1000L
        )

        assertEquals("Production", env.name)
        assertEquals(2, env.variables.size)
        assertFalse(env.variables[0].isSecret)
        assertTrue(env.variables[1].isSecret)
    }
}
