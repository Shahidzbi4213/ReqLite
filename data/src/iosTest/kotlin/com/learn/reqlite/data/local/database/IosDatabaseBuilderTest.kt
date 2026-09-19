package com.learn.reqlite.data.local.database

import com.learn.reqlite.data.local.entity.EnvironmentEntity
import com.learn.reqlite.data.local.entity.VariableEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class IosDatabaseBuilderTest {

    private lateinit var database: ReqLiteDatabase

    @BeforeTest
    fun setup() {
        database = getInMemoryDatabaseBuilder().build()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun databaseBuilder_createsWorkingIosDatabase() = runTest {
        val envDao = database.environmentDao()
        assertNotNull(envDao)

        val env = EnvironmentEntity(
            id = "env_ios_1",
            name = "iOS Test Env",
            color = "#007AFF",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val variable = VariableEntity(
            id = "var_1",
            environmentId = "env_ios_1",
            key = "apiKey",
            value = "ios_key_val",
            isEnabled = true,
            isSecret = false
        )

        envDao.insertEnvironment(env)
        envDao.insertVariables(listOf(variable))

        val retrievedEnv = envDao.getEnvironmentById("env_ios_1")
        assertNotNull(retrievedEnv)
        assertEquals("iOS Test Env", retrievedEnv.name)

        val retrievedVars = envDao.getVariablesForEnvironment("env_ios_1")
        assertEquals(1, retrievedVars.size)
        assertEquals("apiKey", retrievedVars[0].key)
        assertEquals("ios_key_val", retrievedVars[0].value)

        val allEnvs = envDao.getAllEnvironments().first()
        assertEquals(1, allEnvs.size)
    }
}
