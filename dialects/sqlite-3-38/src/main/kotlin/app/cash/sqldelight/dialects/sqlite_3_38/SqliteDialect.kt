package app.cash.sqldelight.dialects.sqlite_3_38

import app.cash.sqldelight.dialects.sqlite_3_38.grammar.SqliteParserUtil
import app.cash.sqldelight.dialects.sqlite_3_35.SqliteDialect as Sqlite335Dialect

class SqliteDialect : Sqlite335Dialect() {
  override fun setup() {
    super.setup()
    SqliteParserUtil.reset()
    SqliteParserUtil.overrideSqlParser()
  }
}
