package com.learn.reqlite.data.repository

import com.learn.reqlite.data.local.dao.EnvironmentDao
import com.learn.reqlite.data.mapper.toDomain
import com.learn.reqlite.data.mapper.toEntity
import com.learn.reqlite.domain.model.Environment
import com.learn.reqlite.domain.model.Variable
import com.learn.reqlite.domain.repository.EnvironmentRepository
import com.learn.reqlite.domain.repository.SecureStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EnvironmentRepositoryImpl(
    private val environmentDao: EnvironmentDao,
    private val secureStorage: SecureStorage
) : EnvironmentRepository {

    override suspend fun insertEnvironment(environment: Environment) {
        environmentDao.insertEnvironment(environment.toEntity())
        
        val variableEntities = environment.variables.map { variable ->
            if (variable.isSecret) {
                secureStorage.saveSecret("env_var_${variable.id}", variable.value)
                variable.copy(value = "").toEntity(environment.id)
            } else {
                variable.toEntity(environment.id)
            }
        }
        if (variableEntities.isNotEmpty()) {
            environmentDao.insertVariables(variableEntities)
        }
    }

    override suspend fun updateEnvironment(environment: Environment) {
        insertEnvironment(environment) // Since OnConflictStrategy is REPLACE
    }

    override suspend fun deleteEnvironment(id: String) {
        // Find existing variables to delete secrets
        val variables = environmentDao.getVariablesForEnvironment(id)
        variables.forEach {
            if (it.isSecret) {
                secureStorage.deleteSecret("env_var_${it.id}")
            }
        }
        environmentDao.deleteEnvironment(id)
    }

    override fun getAllEnvironments(): Flow<List<Environment>> {
        return environmentDao.getAllEnvironments().map { entities ->
            entities.map { entity ->
                val variableEntities = environmentDao.getVariablesForEnvironment(entity.id)
                val variables = variableEntities.map { varEntity ->
                    val domainVar = varEntity.toDomain()
                    if (domainVar.isSecret) {
                        val secretValue = secureStorage.getSecret("env_var_${domainVar.id}") ?: ""
                        domainVar.copy(value = secretValue)
                    } else {
                        domainVar
                    }
                }
                entity.toDomain(variables)
            }
        }
    }

    override suspend fun getEnvironmentById(id: String): Environment? {
        val entity = environmentDao.getEnvironmentById(id) ?: return null
        val variableEntities = environmentDao.getVariablesForEnvironment(id)
        val variables = variableEntities.map { varEntity ->
            val domainVar = varEntity.toDomain()
            if (domainVar.isSecret) {
                val secretValue = secureStorage.getSecret("env_var_${domainVar.id}") ?: ""
                domainVar.copy(value = secretValue)
            } else {
                domainVar
            }
        }
        return entity.toDomain(variables)
    }
}
