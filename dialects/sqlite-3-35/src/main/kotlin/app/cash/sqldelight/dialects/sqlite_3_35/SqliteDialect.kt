package app.cash.sqldelight.dialects.sqlite_3_35

import app.cash.sqldelight.dialect.api.MigrationSquasher
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.sqlite_3_30.SqliteDialect as Sqlite330Dialect
import app.cash.sqldelight.dialects.sqlite_3_35.grammar.SqliteParserUtil

open class SqliteDialect : Sqlite330Dialect() {
  override fun setup() {
    super.setup()
    SqliteParserUtil.reset()
    SqliteParserUtil.overrideSqlParser()
  }

  override fun typeResolver(parentResolver: TypeResolver): TypeResolver {
    return SqliteTypeResolver(parentResolver)
  }

  override fun migrationSquasher(parentSquasher: MigrationSquasher): MigrationSquasher {
    return SqliteMigrationSquasher(super.migrationSquasher(parentSquasher))
  }
}
