package com.squareup.sqldelight.drivers.native

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import co.touchlab.sqliter.DatabaseFileContext.deleteDatabase
import com.squareup.sqldelight.driver.test.DriverTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NativeDriverTest : DriverTest() {
  override fun setupDatabase(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
    val name = "testdb"
    deleteDatabase(name)
    return NativeSqliteDriver(schema, name)
  }

  @Test
  fun canExecuteDriverWithInsertUpdateDeleteUsingReturning() {
    val versionMapper = { cursor: SqlCursor ->
      cursor.next()
      QueryResult.Value(cursor.getString(0)!!)
    }

    val sqliteVersion = driver.executeQuery(-1, "SELECT replace(sqlite_version(), '.', '');", versionMapper, 0).value

    if (sqliteVersion.toInt() < 3350) return

    fun insert(binders: SqlPreparedStatement.() -> Unit, mapper: (SqlCursor) -> QueryResult<Unit>) {
      driver.executeQuery(1, "INSERT INTO test VALUES (?, ?) RETURNING id, value;", mapper, 2, binders)
    }

    fun update(binders: SqlPreparedStatement.() -> Unit, mapper: (SqlCursor) -> QueryResult<Unit>) {
      driver.executeQuery(2, "UPDATE test SET value = ? WHERE id = ? RETURNING value;", mapper, 2, binders)
    }

    fun delete(binders: SqlPreparedStatement.() -> Unit, mapper: (SqlCursor) -> QueryResult<Unit>) {
      driver.executeQuery(2, "DELETE test WHERE id = ? RETURNING value;", mapper, 2, binders)
    }

    insert(
      binders = {
        bindLong(0, 31)
        bindString(1, "Some Value")
      },
      mapper = {
        assertTrue(it.next().value)
        assertEquals(31, it.getLong(0))
        assertEquals("Some Value", it.getString(1))
        QueryResult.Unit
      },
    )

    update(
      binders = {
        bindString(0, "Updated Value")
        bindLong(1, 31)
      },
      mapper = {
        it.next().value
        assertEquals("Updated Value", it.getString(0))
        QueryResult.Unit
      },
    )

    delete(
      binders = {
        bindLong(1, 31)
      },
      mapper = {
        it.next().value
        assertEquals("Updated Value", it.getString(0))
        QueryResult.Unit
      },
    )
  }
}

class NativeDriverMemoryTest : DriverTest() {
  override fun setupDatabase(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
    return inMemoryDriver(schema)
  }
}
