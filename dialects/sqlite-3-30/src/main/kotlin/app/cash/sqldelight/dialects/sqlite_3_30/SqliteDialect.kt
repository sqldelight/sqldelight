package app.cash.sqldelight.dialects.sqlite_3_30

import app.cash.sqldelight.dialects.sqlite_3_25.SqliteDialect as Sqlite325Dialect
import app.cash.sqldelight.dialects.sqlite_3_30.grammar.SqliteParserUtil

/**
 * A dialect for SQLite.
 */
open class SqliteDialect : Sqlite325Dialect() {
  override fun setup() {
    super.setup()
    SqliteParserUtil.reset()
    SqliteParserUtil.overrideSqlParser()
  }
}
