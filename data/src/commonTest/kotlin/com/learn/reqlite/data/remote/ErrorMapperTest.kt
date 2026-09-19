package com.learn.reqlite.data.remote

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import com.learn.reqlite.domain.model.NetworkErrorType

class ErrorMapperTest {

    @Test
    fun testTimeoutExceptions() {
        val mapped1 = mapThrowableToNetworkError(HttpRequestTimeoutException("url", 1000L))
        assertEquals(NetworkErrorType.TIMEOUT, mapped1.type)

        val mapped2 = mapThrowableToNetworkError(ConnectTimeoutException("timeout", Exception()))
        assertEquals(NetworkErrorType.TIMEOUT, mapped2.type)
        
        val mapped3 = mapThrowableToNetworkError(Exception("Socket timeout exception occurred"))
        assertEquals(NetworkErrorType.TIMEOUT, mapped3.type)
    }

    @Test
    fun testDnsExceptions() {
        val mapped1 = mapThrowableToNetworkError(UnresolvedAddressException())
        assertEquals(NetworkErrorType.DNS, mapped1.type)

        val mapped2 = mapThrowableToNetworkError(Exception("Unknown host google.com"))
        assertEquals(NetworkErrorType.DNS, mapped2.type)
    }

    @Test
    fun testTlsExceptions() {
        val mapped1 = mapThrowableToNetworkError(Exception("SSL handshake aborted"))
        assertEquals(NetworkErrorType.TLS, mapped1.type)
        
        val mapped2 = mapThrowableToNetworkError(Exception("Cert path builder failed"))
        assertEquals(NetworkErrorType.TLS, mapped2.type)
    }

    @Test
    fun testConnectionExceptions() {
        val mapped1 = mapThrowableToNetworkError(Exception("Connection refused"))
        assertEquals(NetworkErrorType.CONNECTION, mapped1.type)
    }

    @Test
    fun testCancellation() {
        val mapped1 = mapThrowableToNetworkError(CancellationException("Cancelled by user"))
        assertEquals(NetworkErrorType.CANCELLED, mapped1.type)
    }

    @Test
    fun testUnknownExceptions() {
        val mapped1 = mapThrowableToNetworkError(Exception("Something weird happened"))
        assertEquals(NetworkErrorType.UNKNOWN, mapped1.type)
    }
}
