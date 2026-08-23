package com.learn.reqlite.di

import com.learn.reqlite.data.local.database.ReqLiteDatabase
import com.learn.reqlite.data.local.database.getDatabaseBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<ReqLiteDatabase> {
        getDatabaseBuilder()
            .build()
    }
}
