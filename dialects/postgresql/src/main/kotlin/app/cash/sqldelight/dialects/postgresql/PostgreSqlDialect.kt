package app.cash.sqldelight.dialects.postgresql

import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.postgresql.grammar.PostgreSqlParserUtil
import app.cash.sqldelight.dialects.postgresql.grammar.mixins.ColumnDefMixin
import com.alecstrong.sql.psi.core.SqlParserUtil
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.icons.AllIcons
import com.squareup.kotlinpoet.ClassName

/**
 * Base dialect for JDBC implementations.
 */
class PostgreSqlDialect : SqlDelightDialect {
  override val driverType = ClassName("app.cash.sqldelight.driver.jdbc", "JdbcDriver")
  override val cursorType = ClassName("app.cash.sqldelight.driver.jdbc", "JdbcCursor")
  override val preparedStatementType = ClassName("app.cash.sqldelight.driver.jdbc", "JdbcPreparedStatement")
  override val allowsReferenceCycles = false
  override val icon = AllIcons.Providers.Postgresql

  override fun setup() {
    SqlParserUtil.reset()
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

  override fun typeResolver(parentResolver: TypeResolver): TypeResolver {
    return PostgreSqlTypeResolver(parentResolver)
  }
}
