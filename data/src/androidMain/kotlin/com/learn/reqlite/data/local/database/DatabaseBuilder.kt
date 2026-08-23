package com.learn.reqlite.data.local.database

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

fun getDatabaseBuilder(ctx: Context): RoomDatabase.Builder<ReqLiteDatabase> {
    val appContext = ctx.applicationContext
    return Room.databaseBuilder<ReqLiteDatabase>(
        context = appContext,
        name = "ReqLiteDatabase"
    )
}
