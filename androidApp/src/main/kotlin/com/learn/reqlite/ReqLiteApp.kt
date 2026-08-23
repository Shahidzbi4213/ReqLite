package com.learn.reqlite

import android.app.Application
import com.learn.reqlite.di.initKoin

class ReqLiteApp : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin()
    }
}
