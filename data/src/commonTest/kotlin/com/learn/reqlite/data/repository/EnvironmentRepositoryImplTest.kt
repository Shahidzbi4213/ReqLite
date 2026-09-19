package com.learn.reqlite.data.repository

import com.learn.reqlite.data.local.dao.EnvironmentDao
import com.learn.reqlite.data.local.entity.EnvironmentEntity
import com.learn.reqlite.data.local.entity.VariableEntity
import com.learn.reqlite.domain.model.Environment
import com.learn.reqlite.domain.model.Variable
import com.learn.reqlite.domain.repository.SecureStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeSecureStorage : SecureStorage {
    private val storage = mutableMapOf<String, String>()

    override suspend fun saveSecret(key: String, value: String) {
        storage[key] = value
    }

    override suspend fun getSecret(key: String): String? {
        return storage[key]
    }

    override suspend fun deleteSecret(key: String) {
        storage.remove(key)
    }
}

class FakeEnvironmentDao : EnvironmentDao {
    private val environments = mutableMapOf<String, EnvironmentEntity>()
    private val variables = mutableListOf<VariableEntity>()
    private val environmentsFlow = MutableStateFlow<List<EnvironmentEntity>>(emptyList())

    override suspend fun insertEnvironment(environment: EnvironmentEntity) {
        environments[environment.id] = environment
        environmentsFlow.value = environments.values.toList()
    }

    override suspend fun insertVariables(newVars: List<VariableEntity>) {
        variables.addAll(newVars)
    }

    override fun getAllEnvironments(): Flow<List<EnvironmentEntity>> = environmentsFlow

    override suspend fun getEnvironmentById(id: String): EnvironmentEntity? = environments[id]

    override suspend fun getVariablesForEnvironment(environmentId: String): List<VariableEntity> {
        return variables.filter { it.environmentId == environmentId }
    }

    override suspend fun deleteEnvironment(id: String) {
        environments.remove(id)
        variables.removeAll { it.environmentId == id }
        environmentsFlow.value = environments.values.toList()
    }
}

class EnvironmentRepositoryImplTest {
    @Test
    fun testInsertAndGetEnvironmentWithSecrets() = runTest {
        val dao = FakeEnvironmentDao()
        val secureStorage = FakeSecureStorage()
        val repo = EnvironmentRepositoryImpl(dao, secureStorage)

        val env = Environment(
            id = "e1",
            name = "Test Env",
            color = "#fff",
            variables = listOf(
                Variable("v1", "normal", "value", isEnabled = true, isSecret = false),
                Variable("v2", "secret", "my-secret-token", isEnabled = true, isSecret = true)
            ),
            createdAt = 100L,
            updatedAt = 100L
        )

        repo.insertEnvironment(env)

        val retrieved = repo.getEnvironmentById("e1")
        assertEquals(env.id, retrieved?.id)
        assertEquals(2, retrieved?.variables?.size)
        
        val secretVar = retrieved?.variables?.find { it.key == "secret" }
        assertEquals("my-secret-token", secretVar?.value)
        assertEquals(true, secretVar?.isSecret)
        
        // Verify secure storage was used
        assertEquals("my-secret-token", secureStorage.getSecret("env_var_v2"))
        
        // Verify DAO stored empty string
        val rawVar = dao.getVariablesForEnvironment("e1").find { it.key == "secret" }
        assertEquals("", rawVar?.value)
    }
}
