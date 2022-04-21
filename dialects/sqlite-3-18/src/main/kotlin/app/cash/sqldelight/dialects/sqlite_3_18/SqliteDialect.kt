package app.cash.sqldelight.dialects.sqlite_3_18

import app.cash.sqldelight.dialect.api.ConnectionManager
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.sqlite_3_18.grammar.SqliteParserUtil
import app.cash.sqldelight.dialects.sqlite_3_18.grammar.mixins.ColumnDefMixin
import app.cash.sqldelight.dialects.sqlite_3_18.grammar.mixins.StatementValidatorMixin
import app.cash.sqldelight.dialects.sqlite_3_18.grammar.psi.SqliteTypes
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
 * A dialect for SQLite.
 */
open class SqliteDialect : SqlDelightDialect {
  override val driverType = ClassName("app.cash.sqldelight.db", "SqlDriver")
  override val cursorType = ClassName("app.cash.sqldelight.db", "SqlCursor")
  override val preparedStatementType = ClassName("app.cash.sqldelight.db", "SqlPreparedStatement")
  override val isSqlite = true
  override val icon = AllIcons.Providers.Sqlite
  override val migrationStrategy = SqliteMigrationStrategy()

  override fun setup() {
    Timber.i("Setting up SQLite Dialect")
    SqlParserUtil.reset()

    registerTypeHolder()

    SqliteParserUtil.reset()
    SqliteParserUtil.overrideSqlParser()

    val currentElementCreation = SqliteParserUtil.createElement
    SqliteParserUtil.createElement = {
      when (it.elementType) {
        SqlTypes.COLUMN_DEF -> ColumnDefMixin(it)
        SqlTypes.STMT -> StatementValidatorMixin(it)
        else -> currentElementCreation(it)
      }
    }
  }

  override fun connectionManager() = SqliteConnectionManager()

  private fun registerTypeHolder() {
    ApplicationManager.getApplication()?.apply {
      if (extensionArea.hasExtensionPoint(StubElementTypeHolderEP.EP_NAME)) {
        val exPoint = extensionArea.getExtensionPoint(StubElementTypeHolderEP.EP_NAME)
        if (!exPoint.extensions().anyMatch { it.holderClass == SqliteTypes::class.java.name }) {
          Timber.i("Registering Stub extension point")
          exPoint.registerExtension(
            StubElementTypeHolderEP().apply {
              holderClass = SqliteTypes::class.java.name
            },
            PluginManagerCore.getPlugin(PluginId.getId("com.squareup.sqldelight"))!!,
            this
          )
          Timber.i("Registered Stub extension point")
        }
      }
    }
  }

  override fun typeResolver(parentResolver: TypeResolver): TypeResolver {
    return SqliteTypeResolver(parentResolver)
  }
}
