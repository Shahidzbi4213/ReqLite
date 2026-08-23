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

expect val platformModule: Module

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
    
    single { get<com.learn.reqlite.data.local.database.ReqLiteDatabase>().requestDao() }
    single { get<com.learn.reqlite.data.local.database.ReqLiteDatabase>().environmentDao() }
    single { get<com.learn.reqlite.data.local.database.ReqLiteDatabase>().historyDao() }
    single { get<com.learn.reqlite.data.local.database.ReqLiteDatabase>().collectionDao() }
    single { get<com.learn.reqlite.data.local.database.ReqLiteDatabase>().folderDao() }
}

val domainModule = module {
}

fun initKoin() = initKoin(emptyList())

fun initKoin(extraModules: List<Module>) {
    startKoin {
        modules(
            dataModule,
            domainModule,
            platformModule,
            *extraModules.toTypedArray(),
        )
    }
}
