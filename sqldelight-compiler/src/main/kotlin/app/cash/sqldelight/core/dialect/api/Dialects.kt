package app.cash.sqldelight.core.dialect.api

import app.cash.sqldelight.core.dialect.postgresql.PostgresSqlDelightDialect
import app.cash.sqldelight.core.dialect.sqlite.SqliteSqlDelightDialect
import com.alecstrong.sql.psi.core.DialectPreset
import com.alecstrong.sql.psi.core.DialectPreset.HSQL
import com.alecstrong.sql.psi.core.DialectPreset.MYSQL
import com.alecstrong.sql.psi.core.DialectPreset.POSTGRESQL
import com.alecstrong.sql.psi.core.DialectPreset.SQLITE_3_18
import com.alecstrong.sql.psi.core.DialectPreset.SQLITE_3_24
import com.alecstrong.sql.psi.core.DialectPreset.SQLITE_3_25
import com.alecstrong.sql.psi.core.DialectPreset.SQLITE_3_35

/**
 * Temporary shim to map sql-psi [DialectPreset]s to [SqlDelightDialect]s. This is needed because the Gradle plugin
 * doesn't yet have a new API to provide more information about the dialect/driver implementation. The intention is for
 * all hard references in the compiler to be replaced with the dialect API.
 *
 * See https://github.com/cashapp/sqldelight/pull/2918#pullrequestreview-915124435 and
 * https://github.com/cashapp/sqldelight/issues/2128 and
 * https://github.com/cashapp/sqldelight/issues/2821#issuecomment-1039562786
 */
fun DialectPreset.toSqlDelightDialect(): SqlDelightDialect = when (this) {
  HSQL, MYSQL -> JdbcSqlDelightDialect()
  POSTGRESQL -> PostgresSqlDelightDialect()
  SQLITE_3_18, SQLITE_3_24, SQLITE_3_25, SQLITE_3_35 -> SqliteSqlDelightDialect
}
