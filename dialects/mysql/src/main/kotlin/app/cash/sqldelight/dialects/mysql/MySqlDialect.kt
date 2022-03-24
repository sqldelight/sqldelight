package app.cash.sqldelight.dialects.mysql

import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.dialect.api.TypeResolver
import com.alecstrong.sql.psi.core.DialectPreset.MYSQL
import com.intellij.icons.AllIcons
import com.squareup.kotlinpoet.ClassName

/**
 * Base dialect for JDBC implementations.
 */
class MySqlDialect : SqlDelightDialect {
  override val driverType = ClassName("app.cash.sqldelight.driver.jdbc", "JdbcDriver")
  override val cursorType = ClassName("app.cash.sqldelight.driver.jdbc", "JdbcCursor")
  override val preparedStatementType = ClassName("app.cash.sqldelight.driver.jdbc", "JdbcPreparedStatement")
  override val icon = AllIcons.Providers.Mysql

  override fun setup() {
    MYSQL.setup()
  }

  override fun typeResolver(parentResolver: TypeResolver): TypeResolver {
    return MySqlTypeResolver(parentResolver)
  }
}
