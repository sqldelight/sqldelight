package app.cash.sqldelight.core.dialect.postgresql

import app.cash.sqldelight.core.dialect.api.JdbcSqlDelightDialect

/**
 * A dialect for PostgreSQL.
 */
class PostgresSqlDelightDialect : JdbcSqlDelightDialect() {
  override val allowsReferenceCycles = false
}
