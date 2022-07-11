package app.cash.sqldelight.dialects.mysql

import app.cash.sqldelight.dialect.api.ConnectionManager
import app.cash.sqldelight.dialect.api.MigrationSquasher
import app.cash.sqldelight.dialect.api.RuntimeTypes
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.mysql.grammar.MySqlParserUtil
import app.cash.sqldelight.dialects.mysql.grammar.mixins.ColumnDefMixin
import app.cash.sqldelight.dialects.mysql.grammar.mixins.MySqlBinaryEqualityExpr
import app.cash.sqldelight.dialects.mysql.ide.MySqlConnectionManager
import com.alecstrong.sql.psi.core.SqlParserUtil
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.icons.AllIcons
import com.squareup.kotlinpoet.ClassName

/**
 * Base dialect for JDBC implementations.
 */
class MySqlDialect : SqlDelightDialect {
  override val runtimeTypes: RuntimeTypes = RuntimeTypes(
    ClassName("app.cash.sqldelight.driver.jdbc", "JdbcCursor"),
    ClassName("app.cash.sqldelight.driver.jdbc", "JdbcPreparedStatement"),
  )

  override val asyncRuntimeTypes: RuntimeTypes = RuntimeTypes(
    ClassName("app.cash.sqldelight.driver.r2dbc", "R2dbcCursor"),
    ClassName("app.cash.sqldelight.driver.r2dbc", "R2dbcPreparedStatement"),
  )

  override val icon = AllIcons.Providers.Mysql
  override val connectionManager: ConnectionManager by lazy { MySqlConnectionManager() }

  override fun setup() {
    SqlParserUtil.reset()
    MySqlParserUtil.reset()
    MySqlParserUtil.overrideSqlParser()

    val currentElementCreation = MySqlParserUtil.createElement
    MySqlParserUtil.createElement = {
      when (it.elementType) {
        SqlTypes.COLUMN_DEF -> ColumnDefMixin(it)
        SqlTypes.BINARY_EQUALITY_EXPR -> MySqlBinaryEqualityExpr(it)
        else -> currentElementCreation(it)
      }
    }
  }

  override fun typeResolver(parentResolver: TypeResolver): TypeResolver {
    return MySqlTypeResolver(parentResolver)
  }

  override fun migrationSquasher(parentSquasher: MigrationSquasher): MigrationSquasher {
    return MySqlMigrationSquasher(parentSquasher)
  }
}
