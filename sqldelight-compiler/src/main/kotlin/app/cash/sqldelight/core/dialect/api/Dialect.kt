package app.cash.sqldelight.core.dialect.api

import com.squareup.kotlinpoet.ClassName

interface Dialect {

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
}
