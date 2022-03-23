package app.cash.sqldelight.dialects.sqlite_3_24

import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.sqlite_3_18.SqliteTypeResolver
import com.alecstrong.sql.psi.core.DialectPreset.SQLITE_3_24
import com.squareup.kotlinpoet.ClassName

/**
 * A dialect for SQLite.
 */
class SqliteDialect : SqlDelightDialect {
  override val driverType = ClassName("app.cash.sqldelight.db", "SqlDriver")
  override val cursorType = ClassName("app.cash.sqldelight.db", "SqlCursor")
  override val preparedStatementType = ClassName("app.cash.sqldelight.db", "SqlPreparedStatement")
  override val preset = SQLITE_3_24

  override fun typeResolver(parentResolver: TypeResolver): TypeResolver {
    return SqliteTypeResolver(parentResolver)
  }
}
