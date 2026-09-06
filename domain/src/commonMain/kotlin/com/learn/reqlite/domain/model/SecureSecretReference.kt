package com.learn.reqlite.domain.model

data class SecureSecretReference(
    val id: String,
    val key: String,
    val keystoreId: String
)
