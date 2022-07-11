package app.cash.sqldelight.dialects.hsql

import app.cash.sqldelight.dialect.api.RuntimeTypes
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.hsql.grammar.HsqlParserUtil
import app.cash.sqldelight.dialects.hsql.grammar.mixins.ColumnDefMixin
import com.alecstrong.sql.psi.core.SqlParserUtil
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.icons.AllIcons
import com.squareup.kotlinpoet.ClassName

/**
 * Base dialect for JDBC implementations.
 */
class HsqlDialect : SqlDelightDialect {
  override val runtimeTypes: RuntimeTypes = RuntimeTypes(
    ClassName("app.cash.sqldelight.driver.jdbc", "JdbcCursor"),
    ClassName("app.cash.sqldelight.driver.jdbc", "JdbcPreparedStatement"),
  )

  override val asyncRuntimeTypes: RuntimeTypes
    get() = throw UnsupportedOperationException("HSQL does not support an async driver")

  override val icon = AllIcons.Providers.Hsqldb

  override fun setup() {
    SqlParserUtil.reset()
    HsqlParserUtil.reset()
    HsqlParserUtil.overrideSqlParser()

    val currentElementCreation = HsqlParserUtil.createElement
    HsqlParserUtil.createElement = {
      when (it.elementType) {
        SqlTypes.COLUMN_DEF -> ColumnDefMixin(it)
        else -> currentElementCreation(it)
      }
    }
  }

  override fun typeResolver(parentResolver: TypeResolver): TypeResolver {
    return HsqlTypeResolver(parentResolver)
  }
}
