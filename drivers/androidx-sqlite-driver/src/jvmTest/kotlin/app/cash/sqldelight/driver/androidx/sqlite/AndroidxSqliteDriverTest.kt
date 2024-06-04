package app.cash.sqldelight.driver.androidx.sqlite

import androidx.sqlite.SQLiteException
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.db.use
import com.squareup.sqldelight.driver.test.DriverTest
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class AndroidxSqliteDriverTest : DriverTest() {
  override fun setupDatabase(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
    return AndroidxSqliteDriver(BundledSQLiteDriver(), name = null, schema)
  }

  private fun useSingleItemCacheDriver(block: (AndroidxSqliteDriver) -> Unit) {
    AndroidxSqliteDriver(BundledSQLiteDriver(), name = null, schema, cacheSize = 1).use(block)
  }

  @Test
  fun `cached statement can be reused`() {
    useSingleItemCacheDriver { driver ->
      lateinit var bindable: SqlPreparedStatement
      driver.executeQuery(2, "SELECT * FROM test", { QueryResult.Unit }, 0, { bindable = this })

      driver.executeQuery(
        2,
        "SELECT * FROM test",
        { QueryResult.Unit },
        0,
        {
          assertSame(bindable, this)
        },
      )
    }
  }

  @Test
  fun `cached statement is evicted and closed`() {
    useSingleItemCacheDriver { driver ->
      lateinit var bindable: SqlPreparedStatement
      driver.executeQuery(2, "SELECT * FROM test", { QueryResult.Unit }, 0, { bindable = this })

      driver.executeQuery(3, "SELECT * FROM test", { QueryResult.Unit }, 0)

      driver.executeQuery(
        2,
        "SELECT * FROM test",
        { QueryResult.Unit },
        0,
        {
          assertNotSame(bindable, this)
        },
      )
    }
  }

  @Test
  fun `uncached statement is closed`() {
    useSingleItemCacheDriver { driver ->
      lateinit var bindable: AndroidxStatement
      driver.execute(null, "SELECT * FROM test", 0) {
        bindable = this as AndroidxStatement
      }

      try {
        bindable.execute()
        throw AssertionError("Expected an IllegalStateException (attempt to re-open an already-closed object)")
      } catch (ignored: SQLiteException) {
      }
    }
  }
}
