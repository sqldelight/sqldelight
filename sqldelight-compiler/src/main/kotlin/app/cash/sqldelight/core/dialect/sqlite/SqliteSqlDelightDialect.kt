package app.cash.sqldelight.core.dialect.sqlite

import app.cash.sqldelight.core.dialect.api.SqlDelightDialect
import app.cash.sqldelight.core.lang.CURSOR_TYPE
import app.cash.sqldelight.core.lang.DRIVER_TYPE
import app.cash.sqldelight.core.lang.PREPARED_STATEMENT_TYPE

/**
 * A dialect for SQLite.
 */
object SqliteSqlDelightDialect : SqlDelightDialect {
  override val driverType get() = DRIVER_TYPE
  override val cursorType get() = CURSOR_TYPE
  override val preparedStatementType get() = PREPARED_STATEMENT_TYPE
}
