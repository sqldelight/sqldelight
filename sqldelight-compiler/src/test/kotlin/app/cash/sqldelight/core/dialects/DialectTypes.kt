package app.cash.sqldelight.core.dialects

import app.cash.sqldelight.core.compiler.QueryGenerator
import app.cash.sqldelight.core.compiler.SelectQueryGenerator
import app.cash.sqldelight.core.dialect.api.JdbcSqlDelightDialect
import app.cash.sqldelight.core.dialect.api.SqlDelightDialect
import app.cash.sqldelight.core.dialect.sqlite.SqliteSqlDelightDialect
import com.alecstrong.sql.psi.core.DialectPreset
import com.alecstrong.sql.psi.core.DialectPreset.HSQL
import com.alecstrong.sql.psi.core.DialectPreset.MYSQL
import com.alecstrong.sql.psi.core.DialectPreset.POSTGRESQL
import com.alecstrong.sql.psi.core.DialectPreset.SQLITE_3_18
import com.alecstrong.sql.psi.core.DialectPreset.SQLITE_3_24
import com.alecstrong.sql.psi.core.DialectPreset.SQLITE_3_25
import com.alecstrong.sql.psi.core.DialectPreset.SQLITE_3_35

internal val DialectPreset.textType
  get() = when (this) {
    MYSQL, SQLITE_3_24, SQLITE_3_18, SQLITE_3_25, SQLITE_3_35, POSTGRESQL -> "TEXT"
    HSQL -> "VARCHAR(8)"
  }

internal val DialectPreset.intType
  get() = when (this) {
    MYSQL, SQLITE_3_24, SQLITE_3_18, SQLITE_3_25, SQLITE_3_35, POSTGRESQL, HSQL -> "INTEGER"
  }

/**
 * The [check] statement generated for the prepared statement type in the binder lambda for this dialect.
 *
 * See [QueryGenerator].
 */
internal val SqlDelightDialect.binderCheck
  get() = when (this) {
    is SqliteSqlDelightDialect -> ""
    is JdbcSqlDelightDialect -> "check(this is app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement)\n    "
    else -> throw IllegalStateException("Unknown dialect: $this")
  }

/**
 * The [check] statement generated for the cursor type in the mapper lambda for this dialect.
 *
 * See [SelectQueryGenerator].
 */
internal val SqlDelightDialect.cursorCheck
  get() = when (this) {
    is SqliteSqlDelightDialect -> ""
    is JdbcSqlDelightDialect -> "check(cursor is app.cash.sqldelight.driver.jdbc.JdbcCursor)\n    "
    else -> throw IllegalStateException("Unknown dialect: $this")
  }
