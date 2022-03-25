package app.cash.sqldelight.dialects.sqlite_3_25

import app.cash.sqldelight.dialects.sqlite_3_25.grammar.SqliteParserUtil
import app.cash.sqldelight.dialects.sqlite_3_24.SqliteDialect as Sqlite324Dialect

open class SqliteDialect : Sqlite324Dialect() {
  override fun setup() {
    super.setup()
    SqliteParserUtil.reset()
    SqliteParserUtil.overrideSqlParser()
  }
}
