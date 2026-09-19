package com.learn.reqlite.domain.repository

interface SecureStorage {
    suspend fun saveSecret(key: String, value: String)
    suspend fun getSecret(key: String): String?
    suspend fun deleteSecret(key: String)
}
