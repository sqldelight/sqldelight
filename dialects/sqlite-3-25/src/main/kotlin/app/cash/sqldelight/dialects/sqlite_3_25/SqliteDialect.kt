package app.cash.sqldelight.dialects.sqlite_3_25

import app.cash.sqldelight.dialect.api.MigrationSquasher
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.sqlite_3_24.SqliteDialect as Sqlite324Dialect
import app.cash.sqldelight.dialects.sqlite_3_25.grammar.SqliteParserUtil

open class SqliteDialect : Sqlite324Dialect() {
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
