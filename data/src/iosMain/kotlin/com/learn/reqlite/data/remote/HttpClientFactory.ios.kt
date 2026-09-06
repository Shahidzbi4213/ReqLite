package com.learn.reqlite.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun createHttpClient(enableLogging: Boolean): HttpClient {
    return HttpClient(Darwin) {
        configureCommon(enableLogging)
        
        engine {
            configureRequest {
                setAllowsCellularAccess(true)
            }
        }
    }
}
