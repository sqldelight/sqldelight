package app.cash.sqldelight.paging3

import app.cash.sqldelight.db.SqlDriver

interface DbTest {
  suspend fun setup(driver: SqlDriver)
}
