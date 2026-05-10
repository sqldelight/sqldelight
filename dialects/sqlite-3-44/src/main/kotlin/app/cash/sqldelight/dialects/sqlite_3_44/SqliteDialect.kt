package app.cash.sqldelight.dialects.sqlite_3_44

import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.sqlite_3_38.SqliteDialect as Sqlite338Dialect
import app.cash.sqldelight.dialects.sqlite_3_44.grammar.SqliteParserUtil as Sqlite344ParserUtil

class SqliteDialect : Sqlite338Dialect() {
  override fun setup() {
    super.setup()
    Sqlite344ParserUtil.reset()
    Sqlite344ParserUtil.overrideSqlParser()
  }

  override fun typeResolver(parentResolver: TypeResolver): TypeResolver {
    return SqliteTypeResolver(parentResolver)
  }
}
