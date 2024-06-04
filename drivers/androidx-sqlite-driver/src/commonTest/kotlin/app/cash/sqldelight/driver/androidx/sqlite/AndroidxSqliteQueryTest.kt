package app.cash.sqldelight.driver.androidx.sqlite

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

class AndroidxSqliteQueryTest : CommonQueryTest() {
  override fun setupDatabase(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
    return AndroidxSqliteDriver(androidxSqliteTestDriver(), name = null, schema)
  }
}
