package com.learn.reqlite.di

import com.learn.reqlite.data.engine.RequestExecutionEngineImpl
import com.learn.reqlite.data.local.database.ReqLiteDatabase
import com.learn.reqlite.data.repository.CollectionRepositoryImpl
import com.learn.reqlite.data.repository.EnvironmentRepositoryImpl
import com.learn.reqlite.data.repository.HistoryRepositoryImpl
import com.learn.reqlite.data.repository.RequestRepositoryImpl
import com.learn.reqlite.domain.engine.RequestExecutionEngine
import com.learn.reqlite.domain.repository.CollectionRepository
import com.learn.reqlite.domain.repository.EnvironmentRepository
import com.learn.reqlite.domain.repository.HistoryRepository
import com.learn.reqlite.domain.repository.RequestRepository
import com.learn.reqlite.domain.usecase.RequestValidator
import com.learn.reqlite.domain.usecase.VariableResolver
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

    single<RequestRepository> { RequestRepositoryImpl(get()) }
    single<EnvironmentRepository> { EnvironmentRepositoryImpl(get(), get()) }
    single<HistoryRepository> { HistoryRepositoryImpl(get()) }
    single<CollectionRepository> { CollectionRepositoryImpl(get()) }

    single { VariableResolver() }
    single { RequestValidator(get()) }
    single<RequestExecutionEngine> {
        RequestExecutionEngineImpl(
            httpClient = get(),
            requestRepository = get(),
            environmentRepository = get(),
            secureStorage = get(),
            variableResolver = get(),
            requestValidator = get(),
            historyRepository = get()
        )
    }
}
