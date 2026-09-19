package com.learn.reqlite.domain.model

data class Environment(
    val id: String,
    val name: String,
    val color: String? = null,
    val variables: List<Variable> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long
)
