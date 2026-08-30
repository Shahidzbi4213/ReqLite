package com.learn.reqlite.data.local.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun getDatabaseBuilder(customPath: String? = null): RoomDatabase.Builder<ReqLiteDatabase> {
    val dbFile = if (customPath != null) {
        customPath
    } else {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null
        )
        requireNotNull(documentDirectory?.path) + "/ReqLiteDatabase.db"
    }

    return Room.databaseBuilder<ReqLiteDatabase>(
        name = dbFile
    ).setDriver(BundledSQLiteDriver())
}

fun getInMemoryDatabaseBuilder(): RoomDatabase.Builder<ReqLiteDatabase> {
    return Room.inMemoryDatabaseBuilder<ReqLiteDatabase>()
        .setDriver(BundledSQLiteDriver())
}
