package com.learn.reqlite.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun createHttpClient(enableLogging: Boolean): HttpClient {
    return HttpClient(OkHttp) {
        configureCommon(enableLogging)
        
        engine {
            config {
                retryOnConnectionFailure(true)
                // Additional OkHttp configurations can go here
            }
        }
    }
}
