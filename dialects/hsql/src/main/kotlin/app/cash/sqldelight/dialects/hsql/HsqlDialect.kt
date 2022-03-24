package app.cash.sqldelight.dialects.hsql

import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.dialect.api.TypeResolver
import com.alecstrong.sql.psi.core.DialectPreset.HSQL
import com.intellij.icons.AllIcons
import com.squareup.kotlinpoet.ClassName

/**
 * Base dialect for JDBC implementations.
 */
class HsqlDialect : SqlDelightDialect {
  override val driverType = ClassName("app.cash.sqldelight.driver.jdbc", "JdbcDriver")
  override val cursorType = ClassName("app.cash.sqldelight.driver.jdbc", "JdbcCursor")
  override val preparedStatementType = ClassName("app.cash.sqldelight.driver.jdbc", "JdbcPreparedStatement")
  override val icon = AllIcons.Providers.Hsqldb

  override fun setup() {
    HSQL.setup()
  }

  override fun typeResolver(parentResolver: TypeResolver): TypeResolver {
    return HsqlTypeResolver(parentResolver)
  }
}
