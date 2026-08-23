package com.learn.reqlite.domain.model

data class Collection(
    val id: String,
    val name: String,
    val description: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
