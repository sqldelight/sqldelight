package app.cash.sqldelight.core.dialect.api

import com.squareup.kotlinpoet.ClassName

/**
 * Base dialect for JDBC implementations.
 */
open class JdbcSqlDelightDialect : SqlDelightDialect {
  override val driverType = ClassName("app.cash.sqldelight.driver.jdbc", "JdbcDriver")
  override val cursorType = ClassName("app.cash.sqldelight.driver.jdbc", "JdbcCursor")
  override val preparedStatementType = ClassName("app.cash.sqldelight.driver.jdbc", "JdbcPreparedStatement")
}
