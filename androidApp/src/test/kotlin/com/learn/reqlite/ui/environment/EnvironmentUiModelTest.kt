package com.learn.reqlite.ui.environment

import com.learn.reqlite.domain.model.Environment
import com.learn.reqlite.domain.model.Variable
import org.junit.Assert.*
import org.junit.Test

class EnvironmentUiModelTest {

    @Test
    fun isProtectedName_identifiesProductionVariants() {
        assertTrue(EnvironmentUiModel.isProtectedName("Production"))
        assertTrue(EnvironmentUiModel.isProtectedName("prod"))
        assertTrue(EnvironmentUiModel.isProtectedName("My-Prod-Env"))
        assertTrue(EnvironmentUiModel.isProtectedName("Live API"))
        assertTrue(EnvironmentUiModel.isProtectedName("Main"))

        assertFalse(EnvironmentUiModel.isProtectedName("Development"))
        assertFalse(EnvironmentUiModel.isProtectedName("Dev"))
        assertFalse(EnvironmentUiModel.isProtectedName("Local"))
        assertFalse(EnvironmentUiModel.isProtectedName("Staging"))
        assertFalse(EnvironmentUiModel.isProtectedName("QA"))
    }

    @Test
    fun fromDomain_mapsCorrectly() {
        val domainEnv = Environment(
            id = "env_prod",
            name = "Production US-East",
            color = "#FF0000",
            variables = listOf(
                Variable(id = "v1", key = "baseUrl", value = "https://api.prod.com"),
                Variable(id = "v2", key = "apiKey", value = "secret_123", isSecret = true)
            ),
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val uiModel = EnvironmentUiModel.fromDomain(domainEnv)

        assertEquals("env_prod", uiModel.id)
        assertEquals("Production US-East", uiModel.name)
        assertEquals("#FF0000", uiModel.color)
        assertTrue(uiModel.isProtected)
        assertTrue(uiModel.requireConfirmation)
        assertEquals(2, uiModel.variableCount)
    }

    @Test
    fun fromDomain_standardEnvironment() {
        val domainEnv = Environment(
            id = "env_dev",
            name = "Development",
            variables = emptyList(),
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val uiModel = EnvironmentUiModel.fromDomain(domainEnv)

        assertEquals("env_dev", uiModel.id)
        assertEquals("Development", uiModel.name)
        assertFalse(uiModel.isProtected)
        assertFalse(uiModel.requireConfirmation)
        assertEquals(0, uiModel.variableCount)
    }
}
