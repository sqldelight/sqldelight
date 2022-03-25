package app.cash.sqldelight.dialects.sqlite_3_18

import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.sqlite_3_18.grammar.SqliteParserUtil
import com.alecstrong.sql.psi.core.SqlParserUtil
import com.alecstrong.sql.psi.core.psi.SqlTypes
import app.cash.sqldelight.dialects.sqlite_3_18.grammar.mixins.ColumnDefMixin
import app.cash.sqldelight.dialects.sqlite_3_18.grammar.mixins.StatementValidatorMixin
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
    SqlParserUtil.reset()
    SqliteParserUtil.reset()
    SqliteParserUtil.overrideSqlParser()

    val currentElementCreation = SqliteParserUtil.createElement
    SqliteParserUtil.createElement = {
      when (it.elementType) {
        SqlTypes.COLUMN_DEF -> ColumnDefMixin(it)
        SqlTypes.STMT -> StatementValidatorMixin(it)
        else -> currentElementCreation(it)
      }
    }
  }

  override fun typeResolver(parentResolver: TypeResolver): TypeResolver {
    return SqliteTypeResolver(parentResolver)
  }
}
