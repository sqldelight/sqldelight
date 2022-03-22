package app.cash.sqldelight.core.dialect.api

import com.squareup.kotlinpoet.ClassName

interface SqlDelightDialect {

  /**
   * Dialect-specific implementation of a `SqlDriver`.
   */
  val driverType: ClassName

  /**
   * Dialect-specific implementation of a `SqlCursor`.
   */
  val cursorType: ClassName

  /**
   * Dialect-specific implementation of a `SqlPreparedStatement`.
   */
  val preparedStatementType: ClassName

  /**
   * Whether the dialect supports reference cycles in `CREATE TABLE` statements.
   */
  val allowsReferenceCycles: Boolean get() = true
}
