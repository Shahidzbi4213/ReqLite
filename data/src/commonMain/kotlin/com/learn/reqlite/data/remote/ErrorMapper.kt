package com.learn.reqlite.data.remote

import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import io.ktor.util.network.*
import kotlinx.coroutines.CancellationException

import com.learn.reqlite.domain.model.NetworkErrorType

data class MappedError(
    val type: NetworkErrorType,
    val message: String
)

fun mapThrowableToNetworkError(throwable: Throwable): MappedError {
    val message = throwable.message ?: throwable.toString()
    
    // Ktor specific exceptions
    if (throwable is HttpRequestTimeoutException || throwable is ConnectTimeoutException || throwable is SocketTimeoutException) {
        return MappedError(NetworkErrorType.TIMEOUT, message)
    }
    
    if (throwable is UnresolvedAddressException) {
        return MappedError(NetworkErrorType.DNS, message)
    }
    
    if (throwable is CancellationException) {
        return MappedError(NetworkErrorType.CANCELLED, message)
    }
    
    // Fallback for platform-specific ones by name match since we are in common code
    val name = throwable::class.simpleName ?: ""
    val msg = message.lowercase()
    
    if (name.contains("Timeout") || msg.contains("timeout")) {
        return MappedError(NetworkErrorType.TIMEOUT, message)
    }
    
    if (name.contains("UnknownHost") || msg.contains("unknown host") || name.contains("UnresolvedAddress")) {
        return MappedError(NetworkErrorType.DNS, message)
    }
    
    if (name.contains("SSL") || name.contains("TLS") || name.contains("Cert") || msg.contains("cert") || msg.contains("ssl") || msg.contains("tls")) {
        return MappedError(NetworkErrorType.TLS, message)
    }
    
    if (name.contains("Connect") || name.contains("Socket") || msg.contains("connect") || msg.contains("socket")) {
        return MappedError(NetworkErrorType.CONNECTION, message)
    }
    
    return MappedError(NetworkErrorType.UNKNOWN, message)
}
