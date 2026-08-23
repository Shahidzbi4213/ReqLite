package com.learn.reqlite.domain.model

data class Variable(
    val id: String,
    val key: String,
    val value: String,
    val isEnabled: Boolean = true,
    val isSecret: Boolean = false
)
