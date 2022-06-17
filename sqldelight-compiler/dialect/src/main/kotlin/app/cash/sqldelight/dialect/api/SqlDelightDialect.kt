package app.cash.sqldelight.dialect.api

import com.squareup.kotlinpoet.ClassName
import javax.swing.Icon

interface SqlDelightDialect {
  /**
   * Dialect-specific runtime types
   */
  val runtimeTypes: RuntimeTypes
    get() = RuntimeTypes(
      ClassName("app.cash.sqldelight.db", "SqlCursor"),
      ClassName("app.cash.sqldelight.db", "SqlPreparedStatement"),
    )

  /**
   * Dialect-specific runtime types for async drivers
   */
  val asyncRuntimeTypes: RuntimeTypes
    get() = RuntimeTypes(
      ClassName("app.cash.sqldelight.db", "SqlCursor"),
      ClassName("app.cash.sqldelight.db", "SqlPreparedStatement"),
    )

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

  val connectionManager: ConnectionManager? get() = null

  /**
   * A type resolver specific to this dialect.
   */
  fun typeResolver(parentResolver: TypeResolver): TypeResolver

  fun migrationSquasher(parentSquasher: MigrationSquasher): MigrationSquasher = parentSquasher

  /**
   * Called when this dialect should initialize its parser.
   */
  fun setup()
}
