package com.learn.reqlite.data.local.database

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

fun getDatabaseBuilder(ctx: Context): RoomDatabase.Builder<ReqLiteDatabase> {
    val appContext = ctx.applicationContext
    val dbFile = appContext.getDatabasePath("ReqLiteDatabase.db")
    return Room.databaseBuilder<ReqLiteDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    ).setDriver(BundledSQLiteDriver())
}
