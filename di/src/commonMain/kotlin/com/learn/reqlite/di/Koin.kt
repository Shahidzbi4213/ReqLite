package com.learn.reqlite.di


import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

val dataModule = module {
    single {
        val json = Json { 
            ignoreUnknownKeys = true 
            isLenient = true
        }
        HttpClient {
            install(ContentNegotiation) {
                json(json, contentType = ContentType.Any)
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 30_000
                requestTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }
        }
    }
}

val domainModule = module {
}

fun initKoin() = initKoin(emptyList())

fun initKoin(extraModules: List<Module>) {
    startKoin {
        modules(
            dataModule,
            domainModule,
            *extraModules.toTypedArray(),
        )
    }
}
