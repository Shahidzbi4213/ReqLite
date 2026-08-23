package com.learn.reqlite.data.local.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.learn.reqlite.data.local.entity.CollectionEntity

@Dao
interface CollectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity)

    @Update
    suspend fun updateCollection(collection: CollectionEntity)

    @Delete
    suspend fun deleteCollection(collection: CollectionEntity)

    @Query("SELECT * FROM collections")
    suspend fun getAllCollections(): List<CollectionEntity>

    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getCollectionById(id: String): CollectionEntity?
}
