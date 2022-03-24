package app.cash.sqldelight.dialects.postgresql

import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.dialect.api.TypeResolver
import com.alecstrong.sql.psi.core.DialectPreset.POSTGRESQL
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
    POSTGRESQL.setup()
  }

  override fun typeResolver(parentResolver: TypeResolver): TypeResolver {
    return PostgreSqlTypeResolver(parentResolver)
  }
}
