package com.learn.reqlite.di

import com.learn.reqlite.data.local.database.ReqLiteDatabase
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
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

    single { get<ReqLiteDatabase>().requestDao() }
    single { get<ReqLiteDatabase>().environmentDao() }
    single { get<ReqLiteDatabase>().historyDao() }
    single { get<ReqLiteDatabase>().collectionDao() }
    single { get<ReqLiteDatabase>().folderDao() }
}
