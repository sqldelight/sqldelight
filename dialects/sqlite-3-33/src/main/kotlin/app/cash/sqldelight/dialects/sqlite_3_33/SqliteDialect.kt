package app.cash.sqldelight.dialects.sqlite_3_33

import app.cash.sqldelight.dialects.sqlite_3_30.SqliteDialect as Sqlite330Dialect
import app.cash.sqldelight.dialects.sqlite_3_33.grammar.SqliteParserUtil

/**
 * A dialect for SQLite.
 */
open class SqliteDialect : Sqlite330Dialect() {
  override fun setup() {
    super.setup()
    SqliteParserUtil.reset()
    SqliteParserUtil.overrideSqlParser()
  }
}
