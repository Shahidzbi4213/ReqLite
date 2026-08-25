package com.learn.reqlite.data.local.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.learn.reqlite.domain.repository.SecureStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File

// Use explicit factory instead of delegate to avoid KMP Android compilation issues
private var dataStoreInstance: DataStore<Preferences>? = null

private fun getDataStore(context: Context): DataStore<Preferences> {
    return dataStoreInstance ?: synchronized(AndroidSecureStorage::class) {
        dataStoreInstance ?: PreferenceDataStoreFactory.create(
            produceFile = { File(context.filesDir, "datastore/secure_secrets_ds.preferences_pb") }
        ).also { dataStoreInstance = it }
    }
}

class AndroidSecureStorage(private val context: Context) : SecureStorage {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val alias = "reqlite_secure_storage_key"

    init {
        if (!keyStore.containsAlias(alias)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        return keyStore.getKey(alias, null) as SecretKey
    }

    private fun encrypt(data: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        
        val combined = ByteArray(iv.size + encryptedData.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedData, 0, combined, iv.size, encryptedData.size)
        
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    private fun decrypt(encryptedDataStr: String): String? {
        return try {
            val combined = Base64.decode(encryptedDataStr, Base64.DEFAULT)
            val iv = combined.copyOfRange(0, 12) // GCM IV is 12 bytes
            val encryptedData = combined.copyOfRange(12, combined.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val decryptedData = cipher.doFinal(encryptedData)
            String(decryptedData, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun saveSecret(key: String, value: String) {
        val prefKey = stringPreferencesKey(key)
        val encryptedValue = encrypt(value)
        getDataStore(context).edit { preferences ->
            preferences[prefKey] = encryptedValue
        }
    }

    override suspend fun getSecret(key: String): String? {
        val prefKey = stringPreferencesKey(key)
        val encryptedValue = getDataStore(context).data.map { it[prefKey] }.first()
        return encryptedValue?.let { decrypt(it) }
    }

    override suspend fun deleteSecret(key: String) {
        val prefKey = stringPreferencesKey(key)
        getDataStore(context).edit { preferences ->
            preferences.remove(prefKey)
        }
    }
}
