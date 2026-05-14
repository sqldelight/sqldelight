package app.cash.sqldelight.dialects.sqlite_3_37

import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.sqlite_3_35.SqliteDialect as Sqlite350Dialect
import app.cash.sqldelight.dialects.sqlite_3_37.grammar.SqliteParserUtil

open class SqliteDialect : Sqlite350Dialect() {
  override fun setup() {
    super.setup()
    SqliteParserUtil.reset()
    SqliteParserUtil.overrideSqlParser()
  }

  override fun typeResolver(parentResolver: TypeResolver): TypeResolver {
    return SqliteTypeResolver(parentResolver)
  }
}
