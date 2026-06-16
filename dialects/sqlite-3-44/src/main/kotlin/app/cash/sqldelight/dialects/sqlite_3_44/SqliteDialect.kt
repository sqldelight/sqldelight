package app.cash.sqldelight.dialects.sqlite_3_44

import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.sqlite_3_39.SqliteDialect as Sqlite339Dialect
import app.cash.sqldelight.dialects.sqlite_3_44.grammar.SqliteParserUtil as Sqlite344ParserUtil

class SqliteDialect : Sqlite339Dialect() {
  override fun setup() {
    super.setup()
    Sqlite344ParserUtil.reset()
    Sqlite344ParserUtil.overrideSqlParser()
  }

  override fun typeResolver(parentResolver: TypeResolver): TypeResolver {
    return SqliteTypeResolver(parentResolver)
  }
}
