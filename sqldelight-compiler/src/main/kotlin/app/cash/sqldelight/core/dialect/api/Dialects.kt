package app.cash.sqldelight.core.dialect.api

import app.cash.sqldelight.core.dialect.sqlite.SqliteDialect
import com.alecstrong.sql.psi.core.DialectPreset
import com.alecstrong.sql.psi.core.DialectPreset.HSQL
import com.alecstrong.sql.psi.core.DialectPreset.MYSQL
import com.alecstrong.sql.psi.core.DialectPreset.POSTGRESQL
import com.alecstrong.sql.psi.core.DialectPreset.SQLITE_3_18
import com.alecstrong.sql.psi.core.DialectPreset.SQLITE_3_24
import com.alecstrong.sql.psi.core.DialectPreset.SQLITE_3_25

/**
 * Temporary shim to map sql-psi [DialectPreset]s to [Dialect]s. This is needed because the Gradle plugin doesn't yet
 * have a new API to provide more information about the dialect/driver implementation.
 *
 * See https://github.com/cashapp/sqldelight/issues/2128 and
 * https://github.com/cashapp/sqldelight/issues/2821#issuecomment-1039562786
 */
fun DialectPreset.asDialect(): Dialect = when (this) {
  HSQL, POSTGRESQL, MYSQL -> JdbcDialect()
  SQLITE_3_18, SQLITE_3_24, SQLITE_3_25 -> SqliteDialect
}
