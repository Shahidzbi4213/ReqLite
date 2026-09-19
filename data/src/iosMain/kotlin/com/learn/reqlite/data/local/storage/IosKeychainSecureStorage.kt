package com.learn.reqlite.data.local.storage

import com.learn.reqlite.domain.repository.SecureStorage
import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*

/**
 * Production-ready iOS Keychain backend for SecureStorage.
 * Stores sensitive environment variables and tokens in the iOS Keychain Services
 * with kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly accessibility and graceful in-memory fallback.
 */
class IosKeychainSecureStorage(
    private val serviceName: String = "com.learn.reqlite.secure_storage",
    private val accessGroup: String? = null
) : SecureStorage {

    private val inMemoryFallback = mutableMapOf<String, String>()

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun saveSecret(key: String, value: String) {
        inMemoryFallback[key] = value

        val nsValue = NSString.create(string = value)
        val data = nsValue.dataUsingEncoding(NSUTF8StringEncoding) ?: return

        // First attempt to update existing item
        val query = createBaseQuery(key)
        val updateDict = CFDictionaryCreateMutable(null, 1, null, null)
        CFDictionaryAddValue(updateDict, kSecValueData, CFBridgingRetain(data))

        val updateStatus = SecItemUpdate(query, updateDict)
        CFRelease(query)
        CFRelease(updateDict)

        if (updateStatus == errSecItemNotFound) {
            // Item does not exist, add a new one
            val addQuery = createBaseQuery(key)
            CFDictionaryAddValue(addQuery, kSecValueData, CFBridgingRetain(data))
            CFDictionaryAddValue(addQuery, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)

            val addStatus = SecItemAdd(addQuery, null)
            CFRelease(addQuery)

            if (addStatus == errSecDuplicateItem) {
                // In case of race condition, try update again
                val retryQuery = createBaseQuery(key)
                val retryUpdate = CFDictionaryCreateMutable(null, 1, null, null)
                CFDictionaryAddValue(retryUpdate, kSecValueData, CFBridgingRetain(data))
                SecItemUpdate(retryQuery, retryUpdate)
                CFRelease(retryQuery)
                CFRelease(retryUpdate)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun getSecret(key: String): String? {
        val query = createBaseQuery(key)
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)

        var resultString: String? = null

        memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            CFRelease(query)

            if (status == errSecSuccess && result.value != null) {
                val nsData = CFBridgingRelease(result.value) as? NSData
                if (nsData != null) {
                    val str = NSString.create(data = nsData, encoding = NSUTF8StringEncoding)
                    if (str != null) {
                        resultString = str.toString()
                    }
                }
            }
        }

        return resultString ?: inMemoryFallback[key]
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun deleteSecret(key: String) {
        inMemoryFallback.remove(key)

        val query = createBaseQuery(key)
        SecItemDelete(query)
        CFRelease(query)
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    fun clearAllSecrets() {
        inMemoryFallback.clear()

        val query = CFDictionaryCreateMutable(null, 2, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain(NSString.create(string = serviceName)))
        accessGroup?.let {
            CFDictionaryAddValue(query, kSecAttrAccessGroup, CFBridgingRetain(NSString.create(string = it)))
        }

        SecItemDelete(query)
        CFRelease(query)
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun createBaseQuery(key: String): CFMutableDictionaryRef? {
        val query = CFDictionaryCreateMutable(null, 4, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain(NSString.create(string = serviceName)))
        CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(NSString.create(string = key)))
        accessGroup?.let {
            CFDictionaryAddValue(query, kSecAttrAccessGroup, CFBridgingRetain(NSString.create(string = it)))
        }
        return query
    }
}

/**
 * Typealias for backward compatibility with initial iOS platform setup.
 */
typealias IosSecureStorage = IosKeychainSecureStorage
