package com.learn.reqlite.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.learn.reqlite.data.local.entity.EnvironmentEntity
import com.learn.reqlite.data.local.entity.VariableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EnvironmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnvironment(environment: EnvironmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariables(variables: List<VariableEntity>)

    @Query("SELECT * FROM environments")
    fun getAllEnvironments(): Flow<List<EnvironmentEntity>>

    @Query("SELECT * FROM environments WHERE id = :id")
    suspend fun getEnvironmentById(id: String): EnvironmentEntity?

    @Query("SELECT * FROM variables WHERE environmentId = :environmentId")
    suspend fun getVariablesForEnvironment(environmentId: String): List<VariableEntity>

    @Query("DELETE FROM environments WHERE id = :id")
    suspend fun deleteEnvironment(id: String)
}
