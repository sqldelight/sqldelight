package com.squareup.sqldelight.android

import android.os.Build
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.db.StaleWindowException
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class StaleWindowTest {
  private lateinit var driver: SqlDriver

  private val schema = object : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(
        null,
        """
          CREATE TABLE test_table (
            id INTEGER NOT NULL PRIMARY KEY,
            name TEXT NOT NULL,
            value INTEGER NOT NULL
          )
        """.trimIndent(),
        0,
      )
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: app.cash.sqldelight.db.AfterVersion,
    ): QueryResult.Value<Unit> = QueryResult.Unit
  }

  @Before
  fun setup() {
    driver = AndroidSqliteDriver(schema, getApplicationContext())
  }

  @After
  fun teardown() {
    driver.close()
  }

  @Test
  @Config(sdk = [Build.VERSION_CODES.P])
  fun `concurrent deletes trigger stale window`() {
    driver.close()
    driver = AndroidSqliteDriver(
      schema = schema,
      context = getApplicationContext(),
      windowSizeBytes = 2048L,
    )

    // Insert 100 entries
    for (i in 1..100) {
      driver.execute(
        null,
        "INSERT INTO test_table (id, name, value) VALUES ($i, 'Name$i', ${i * 10})",
        0,
      )
    }

    var exceptionCaught: Exception? = null
    var rowsProcessed = 0

    try {
      driver.executeQuery(
        null,
        "SELECT * FROM test_table ORDER BY id",
        { cursor ->
          while (cursor.next().value) {
            // Use !! to mirror generated mapper behavior. With the fix in place,
            // StaleWindowException should be thrown before any mapper-level NPE.
            cursor.getLong(0)!!
            cursor.getString(1)!!
            cursor.getLong(2)!!

            rowsProcessed++

            // After 20 rows, delete ids > 80 to shrink the window
            if (rowsProcessed == 20) {
              driver.execute(null, "DELETE FROM test_table WHERE id > 80", 0)
            }
          }
          QueryResult.Unit
        },
        0,
      )
    } catch (e: Exception) {
      exceptionCaught = e
    }

    val exception = exceptionCaught ?: run {
      fail("Expected StaleWindowException, but no exception was thrown")
      return
    }

    assertTrue(
      "Expected StaleWindowException, but got ${exception::class.java.name}",
      exception is StaleWindowException,
    )
    assertTrue(
      "Exception message should contain position info",
      exception.message?.contains("position=") == true,
    )
    // We delete rows 80+
    assertEquals(
      "Detected stale window after processing rows",
      80,
      rowsProcessed,
    )
  }
}
