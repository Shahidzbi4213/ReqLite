package com.learn.reqlite.di

import com.learn.reqlite.data.local.database.ReqLiteDatabase
import com.learn.reqlite.data.local.database.getDatabaseBuilder
import com.learn.reqlite.data.local.storage.IosKeychainSecureStorage
import com.learn.reqlite.domain.repository.SecureStorage
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<ReqLiteDatabase> {
        getDatabaseBuilder()
            .build()
    }

    single<SecureStorage> {
        IosKeychainSecureStorage()
    }
}

fun initKoinIos() {
    initKoin(emptyList())
}
