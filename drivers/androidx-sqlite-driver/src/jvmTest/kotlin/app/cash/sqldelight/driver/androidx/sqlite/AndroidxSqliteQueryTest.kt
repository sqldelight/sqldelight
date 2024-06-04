package app.cash.sqldelight.driver.androidx.sqlite

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.squareup.sqldelight.driver.test.QueryTest

class AndroidxSqliteQueryTest : QueryTest() {
  override fun setupDatabase(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
    return AndroidxSqliteDriver(BundledSQLiteDriver(), name = null, schema)
  }
}
