package app.cash.sqldelight.dialects.sqlite_3_24

import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.sqlite_3_24.grammar.SqliteParserUtil
import app.cash.sqldelight.dialects.sqlite_3_18.SqliteDialect as Sqlite318Dialect

open class SqliteDialect : Sqlite318Dialect() {
  override fun setup() {
    super.setup()
    SqliteParserUtil.reset()
    SqliteParserUtil.overrideSqlParser()
  }

  override fun typeResolver(parentResolver: TypeResolver): TypeResolver {
    return SqliteTypeResolver(parentResolver)
  }
}
