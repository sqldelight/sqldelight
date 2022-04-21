package app.cash.sqldelight.dialect.api

import com.squareup.kotlinpoet.ClassName
import javax.swing.Icon

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
   * SQLite receives special treatment in the IDE.
   */
  val isSqlite: Boolean get() = false

  val icon: Icon

  val migrationStrategy: SqlGeneratorStrategy get() = NoOp()

  /**
   * A type resolver specific to this dialect.
   */
  fun typeResolver(parentResolver: TypeResolver): TypeResolver

  /**
   * Called when this dialect should initialize its parser.
   */
  fun setup()

  fun connectionManager(): ConnectionManager? = null
}
