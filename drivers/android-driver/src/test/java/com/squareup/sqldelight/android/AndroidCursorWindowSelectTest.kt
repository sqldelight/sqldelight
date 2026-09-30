/*
 * Copyright (C) 2026 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sqldelight.android

import android.content.res.Resources
import android.database.AbstractWindowedCursor
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidCursorWindowSelectTest {
  @Test fun executeAsListSeesOneSnapshotAcrossCursorWindows() {
    val context = getApplicationContext<android.content.Context>()
    val name = "sqldelight-cursor-window-select-test.db"
    context.deleteDatabase(name)

    // Pad each row so that 16 rows will fill a window. Then use 17 rows so the
    // last row is guaranteed to be in the next window.
    val resource = Resources.getSystem().getIdentifier("config_cursorWindowSize", "integer", "android")
    assertTrue("Android's CursorWindow size resource was not found", resource != 0)
    val windowBytes = Resources.getSystem().getInteger(resource) * 1024L
    val paddingBytes = windowBytes / 16
    val rowCount = 17

    val helper = FrameworkSQLiteOpenHelperFactory().create(
      SupportSQLiteOpenHelper.Configuration.builder(context)
        .name(name)
        .callback(object : SupportSQLiteOpenHelper.Callback(1) {
          override fun onCreate(db: SupportSQLiteDatabase) = Unit
          override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        })
        .build(),
    )
    AndroidSqliteDriver(openHelper = helper).use { driver ->
      val database = helper.writableDatabase

      // Setup the database with rows 0 through rowCount-1, with the required padding
      database.execSQL("CREATE TABLE items (id INTEGER PRIMARY KEY, version INTEGER NOT NULL, padding BLOB NOT NULL)")
      repeat(rowCount) { id ->
        database.execSQL(
          "INSERT INTO items (id, version, padding) VALUES (?, 0, zeroblob(?))",
          arrayOf<Int>(id.toInt(), paddingBytes.toInt()),
        )
      }

      // Verify the rows don't fit in a single CursorWindow
      val firstWindowRows = database.query("SELECT id, version, padding FROM items ORDER BY id").use { cursor ->
        assertTrue(cursor.moveToFirst())
        (cursor as AbstractWindowedCursor).window.numRows
      }
      assertTrue("All $rowCount rows fit in one window", firstWindowRows < rowCount)

      val updaterExecutor = Executors.newSingleThreadExecutor()
      AutoCloseable { updaterExecutor.shutdownNow() }.use {
        val firstRowMappedLatch = CountDownLatch(1)
        val updateStartedLatch = CountDownLatch(1)

        val update = updaterExecutor.submit {
          // Wait until the first row of the query is fetched ...
          assertTrue(firstRowMappedLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))

          // ... Then note that we're starting the update ...
          updateStartedLatch.countDown()

          // ... Then make the actual update (if the query is transactional,
          // this will block until the query is complete)
          database.execSQL("UPDATE items SET version = 1")
        }

        // We're going to check if it's possible to execute a single
        // sqldelight query that captures results from multiple underlying
        // sqlite executions.
        val versions = driver.executeQuery(
          identifier = 1,
          sql = "SELECT id, version, padding FROM items ORDER BY id",
          parameters = 0,
          mapper = { cursor ->
            val versions = mutableListOf<Long>()
            while (cursor.next().value) {
              if (cursor.getLong(0) == 0L) {
                // When we retrieve the first row, count-down the latch, and then wait for the updater thread.
                firstRowMappedLatch.countDown()
                assertTrue(updateStartedLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))

                // Wait for the update to complete
                try {
                  update.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                } catch (e: TimeoutException) {
                  // This is expected when the query is correctly run within a transaction
                }
              }

              // Return the version (which gets updated by the updater thread)
              versions += cursor.getLong(1)!!
            }

            QueryResult.Value(versions)
          },
        ).value

        update.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        assertEquals(rowCount, versions.size)
        val firstChangedRow = versions.indexOfFirst { it != 0L }
        assertEquals(
          "One executeQuery() mixed snapshots after window row $firstWindowRows.",
          -1,
          firstChangedRow,
        )
      }
    }
  }

  companion object {
    const val TIMEOUT_SECONDS = 1L
  }
}
