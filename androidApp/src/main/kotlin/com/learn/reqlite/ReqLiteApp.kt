package com.learn.reqlite

import android.app.Application
import android.content.Context
import com.learn.reqlite.di.initKoin
import com.learn.reqlite.di.appModule
import org.koin.dsl.module

class ReqLiteApp : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin(
            listOf(
                module {
                    single<Context> { this@ReqLiteApp }
                },
                appModule
            )
        )
    }
}
