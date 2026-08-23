package com.learn.reqlite.data.local.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.learn.reqlite.data.local.dao.CollectionDao
import com.learn.reqlite.data.local.dao.FolderDao
import com.learn.reqlite.data.local.entity.CollectionEntity
import com.learn.reqlite.data.local.entity.FolderEntity

import androidx.room3.ConstructedBy
import androidx.room3.RoomDatabaseConstructor

@Database(
    entities = [
        CollectionEntity::class,
        FolderEntity::class
    ],
    version = 1
)
@ConstructedBy(ReqLiteDatabaseConstructor::class)
abstract class ReqLiteDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao
    abstract fun folderDao(): FolderDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object ReqLiteDatabaseConstructor : RoomDatabaseConstructor<ReqLiteDatabase> {
    override fun initialize(): ReqLiteDatabase
}
