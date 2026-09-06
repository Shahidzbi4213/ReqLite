package com.learn.reqlite.data.local.dao

import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import com.learn.reqlite.data.local.database.ReqLiteDatabase
import com.learn.reqlite.data.local.entity.RequestEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RequestDaoTest {
    private lateinit var database: ReqLiteDatabase
    private lateinit var requestDao: RequestDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            ReqLiteDatabase::class.java
        ).allowMainThreadQueries().build()
        requestDao = database.requestDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testInsertAndGetRequest() = runBlocking {
        val request = RequestEntity(
            id = "req_1",
            collectionId = "col_1",
            folderId = null,
            name = "Test Request",
            method = "GET",
            url = "https://example.com",
            createdAt = 12345L,
            updatedAt = 12345L
        )
        // Wait, because of foreign keys, we might need to insert Collection first.
        // We can either disable foreign keys in the builder or insert a collection.
    }
}
