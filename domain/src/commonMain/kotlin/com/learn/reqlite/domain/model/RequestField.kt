package com.learn.reqlite.domain.model

data class RequestField(
    val id: String,
    val key: String,
    val value: String,
    val isEnabled: Boolean = true,
    val description: String? = null
)
