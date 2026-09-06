package com.learn.reqlite.data.local.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class DatabaseBuilderTest {
    @Test
    fun testDatabaseBuilderCreatesSuccessfully() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val builder = getDatabaseBuilder(context)
        assertNotNull(builder)
        
        // Ensure it builds without throwing exceptions
        val db = builder.build()
        assertNotNull(db)
        db.close()
    }
}
