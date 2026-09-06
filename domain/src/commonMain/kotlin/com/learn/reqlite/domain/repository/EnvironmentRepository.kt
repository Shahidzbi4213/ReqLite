package com.learn.reqlite.domain.repository

import com.learn.reqlite.domain.model.Environment
import kotlinx.coroutines.flow.Flow

interface EnvironmentRepository {
    suspend fun insertEnvironment(environment: Environment)
    suspend fun updateEnvironment(environment: Environment)
    suspend fun deleteEnvironment(id: String)
    fun getAllEnvironments(): Flow<List<Environment>>
    suspend fun getEnvironmentById(id: String): Environment?
}
