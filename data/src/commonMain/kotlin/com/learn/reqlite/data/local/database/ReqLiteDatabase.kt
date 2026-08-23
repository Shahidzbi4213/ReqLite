@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.learn.reqlite.data.local.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.learn.reqlite.data.local.dao.CollectionDao
import com.learn.reqlite.data.local.dao.EnvironmentDao
import com.learn.reqlite.data.local.dao.FolderDao
import com.learn.reqlite.data.local.dao.HistoryDao
import com.learn.reqlite.data.local.dao.RequestDao
import com.learn.reqlite.data.local.entity.CollectionEntity
import com.learn.reqlite.data.local.entity.DraftEntity
import com.learn.reqlite.data.local.entity.EnvironmentEntity
import com.learn.reqlite.data.local.entity.FolderEntity
import com.learn.reqlite.data.local.entity.HistoryEntryEntity
import com.learn.reqlite.data.local.entity.RequestBodyEntity
import com.learn.reqlite.data.local.entity.RequestEntity
import com.learn.reqlite.data.local.entity.RequestFieldEntity
import com.learn.reqlite.data.local.entity.ResponseArtifactEntity
import com.learn.reqlite.data.local.entity.VariableEntity

import androidx.room3.ConstructedBy
import androidx.room3.RoomDatabaseConstructor

@Database(
    entities = [
        CollectionEntity::class,
        FolderEntity::class,
        RequestEntity::class,
        RequestFieldEntity::class,
        RequestBodyEntity::class,
        DraftEntity::class,
        EnvironmentEntity::class,
        VariableEntity::class,
        HistoryEntryEntity::class,
        ResponseArtifactEntity::class
    ],
    version = 1
)
@ConstructedBy(ReqLiteDatabaseConstructor::class)
abstract class ReqLiteDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao
    abstract fun folderDao(): FolderDao
    abstract fun requestDao(): RequestDao
    abstract fun environmentDao(): EnvironmentDao
    abstract fun historyDao(): HistoryDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object ReqLiteDatabaseConstructor : RoomDatabaseConstructor<ReqLiteDatabase> {
    override fun initialize(): ReqLiteDatabase
}
