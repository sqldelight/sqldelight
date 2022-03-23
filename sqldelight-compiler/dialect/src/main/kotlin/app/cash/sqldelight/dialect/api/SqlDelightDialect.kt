package app.cash.sqldelight.dialect.api

import com.alecstrong.sql.psi.core.DialectPreset
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

  /**
   * Temporary until DialectPreset is deleted from sql-psi and dialects are moved out of sql-psi.
   */
  val preset: DialectPreset

  /**
   * A type resolver specific to this dialect.
   */
  fun typeResolver(parentResolver: TypeResolver): TypeResolver
}
