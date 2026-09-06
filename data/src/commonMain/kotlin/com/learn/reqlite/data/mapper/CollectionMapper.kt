package com.learn.reqlite.data.mapper

import com.learn.reqlite.data.local.entity.CollectionEntity
import com.learn.reqlite.domain.model.Collection

fun CollectionEntity.toDomain(): Collection {
    return Collection(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Collection.toEntity(): CollectionEntity {
    return CollectionEntity(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
