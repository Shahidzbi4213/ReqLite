package com.learn.reqlite.data.mapper

import com.learn.reqlite.data.local.entity.EnvironmentEntity
import com.learn.reqlite.data.local.entity.VariableEntity
import com.learn.reqlite.domain.model.Environment
import com.learn.reqlite.domain.model.Variable

fun EnvironmentEntity.toDomain(variables: List<Variable>): Environment {
    return Environment(
        id = id,
        name = name,
        color = color,
        variables = variables,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Environment.toEntity(): EnvironmentEntity {
    return EnvironmentEntity(
        id = id,
        name = name,
        color = color,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun VariableEntity.toDomain(): Variable {
    return Variable(
        id = id,
        key = key,
        value = value,
        isEnabled = isEnabled,
        isSecret = isSecret
    )
}

fun Variable.toEntity(environmentId: String): VariableEntity {
    return VariableEntity(
        id = id,
        environmentId = environmentId,
        key = key,
        value = value,
        isEnabled = isEnabled,
        isSecret = isSecret
    )
}
