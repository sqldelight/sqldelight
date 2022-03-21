package app.cash.sqldelight.core.dialect.sqlite

import app.cash.sqldelight.core.dialect.api.SqlDelightDialect
import app.cash.sqldelight.core.lang.CURSOR_TYPE
import app.cash.sqldelight.core.lang.DRIVER_TYPE
import com.squareup.kotlinpoet.ClassName

/**
 * A dialect for SQLite.
 */
object SqliteSqlDelightDialect : SqlDelightDialect {
  override val driverType: ClassName get() = DRIVER_TYPE
  override val cursorType: ClassName get() = CURSOR_TYPE
  override val preparedStatementType = ClassName("app.cash.sqldelight.db", "SqlPreparedStatement")
}
