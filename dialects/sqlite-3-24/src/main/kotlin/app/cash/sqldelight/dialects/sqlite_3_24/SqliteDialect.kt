package app.cash.sqldelight.dialects.sqlite_3_24

import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.sqlite_3_18.SqliteMigrationStrategy
import app.cash.sqldelight.dialects.sqlite_3_24.grammar.SqliteParserUtil
import com.alecstrong.sql.psi.core.DialectPreset.SQLITE_3_18
import com.intellij.icons.AllIcons
import com.squareup.kotlinpoet.ClassName

/**
 * A dialect for SQLite.
 */
open class SqliteDialect : SqlDelightDialect {
  override val driverType = ClassName("app.cash.sqldelight.db", "SqlDriver")
  override val cursorType = ClassName("app.cash.sqldelight.db", "SqlCursor")
  override val preparedStatementType = ClassName("app.cash.sqldelight.db", "SqlPreparedStatement")
  override val isSqlite = true
  override val icon = AllIcons.Providers.Sqlite
  override val migrationStrategy = SqliteMigrationStrategy()

  override fun setup() {
    SQLITE_3_18.setup()
    SqliteParserUtil.reset()
    SqliteParserUtil.overrideSqlParser()
  }

  override fun typeResolver(parentResolver: TypeResolver): TypeResolver {
    return SqliteTypeResolver(parentResolver)
  }
}
