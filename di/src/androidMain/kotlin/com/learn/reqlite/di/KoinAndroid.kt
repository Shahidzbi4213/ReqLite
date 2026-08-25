package com.learn.reqlite.di

import android.content.Context
import com.learn.reqlite.data.local.database.ReqLiteDatabase
import com.learn.reqlite.data.local.database.getDatabaseBuilder
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<ReqLiteDatabase> {
        val context = get<Context>()
        getDatabaseBuilder(context)
            .build()
    }
    
    single<com.learn.reqlite.domain.repository.SecureStorage> {
        com.learn.reqlite.data.local.storage.AndroidSecureStorage(get<Context>())
    }
}
