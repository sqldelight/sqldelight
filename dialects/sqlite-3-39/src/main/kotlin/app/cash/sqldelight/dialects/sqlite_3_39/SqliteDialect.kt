package app.cash.sqldelight.dialects.sqlite_3_39

import app.cash.sqldelight.dialects.sqlite_3_38.SqliteDialect as Sqlite338Dialect
import app.cash.sqldelight.dialects.sqlite_3_39.grammar.SqliteParserUtil
// 3_39 doesn't currently need to override SqliteTypeResolver uses inherited version
open class SqliteDialect : Sqlite338Dialect() {
  override fun setup() {
    super.setup()
    SqliteParserUtil.reset()
    SqliteParserUtil.overrideSqlParser()
  }
}
