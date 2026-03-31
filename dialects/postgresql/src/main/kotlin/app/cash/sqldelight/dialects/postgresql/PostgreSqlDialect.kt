package app.cash.sqldelight.dialects.postgresql

import app.cash.sqldelight.dialect.api.ConnectionManager
import app.cash.sqldelight.dialect.api.MigrationSquasher
import app.cash.sqldelight.dialect.api.RuntimeTypes
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.postgresql.grammar.PostgreSqlParserUtil
import app.cash.sqldelight.dialects.postgresql.grammar.mixins.ColumnDefMixin
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlTypes
import app.cash.sqldelight.dialects.postgresql.ide.PostgresConnectionManager
import com.alecstrong.sql.psi.core.SqlParserUtil
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.icons.AllIcons
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.psi.stubs.StubElementTypeHolderEP
import com.squareup.kotlinpoet.ClassName
import timber.log.Timber

/**
 * Base dialect for JDBC implementations.
 */
class PostgreSqlDialect : SqlDelightDialect {
  override val runtimeTypes: RuntimeTypes = RuntimeTypes(
    ClassName("app.cash.sqldelight.driver.jdbc", "JdbcCursor"),
    ClassName("app.cash.sqldelight.driver.jdbc", "JdbcPreparedStatement"),
  )

  override val asyncRuntimeTypes: RuntimeTypes = RuntimeTypes(
    ClassName("app.cash.sqldelight.driver.r2dbc", "R2dbcCursor"),
    ClassName("app.cash.sqldelight.driver.r2dbc", "R2dbcPreparedStatement"),
  )

  override val allowsReferenceCycles = false
  override val icon = AllIcons.Providers.Postgresql
  override val connectionManager: ConnectionManager by lazy { PostgresConnectionManager() }

  override fun setup() {
    SqlParserUtil.reset()

    registerTypeHolder()

    PostgreSqlParserUtil.reset()
    PostgreSqlParserUtil.overrideSqlParser()

    val currentElementCreation = PostgreSqlParserUtil.createElement
    PostgreSqlParserUtil.createElement = {
      when (it.elementType) {
        SqlTypes.COLUMN_DEF -> ColumnDefMixin(it)
        else -> currentElementCreation(it)
      }
    }
  }

  private fun registerTypeHolder() {
    ApplicationManager.getApplication()?.apply {
      if (extensionArea.hasExtensionPoint(StubElementTypeHolderEP.EP_NAME)) {
        val exPoint = extensionArea.getExtensionPoint(StubElementTypeHolderEP.EP_NAME)
        if (!exPoint.extensions.any { it.holderClass == PostgreSqlTypes::class.java.name }) {
          Timber.i("Registering Stub extension point")
          exPoint.registerExtension(
            StubElementTypeHolderEP().apply {
              holderClass = PostgreSqlTypes::class.java.name
            },
            PluginManagerCore.getPlugin(PluginId.getId("com.squareup.sqldelight"))!!,
            this,
          )
          Timber.i("Registered Stub extension point")
        }
      }
    }
  }

  override fun typeResolver(parentResolver: TypeResolver): TypeResolver {
    return PostgreSqlTypeResolver(parentResolver)
  }

  override fun migrationSquasher(parentSquasher: MigrationSquasher): MigrationSquasher {
    return PostgreSqlMigrationSquasher(parentSquasher)
  }
}
