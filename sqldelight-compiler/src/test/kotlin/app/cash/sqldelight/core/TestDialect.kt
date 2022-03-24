package app.cash.sqldelight.core

import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.dialects.hsql.HsqlDialect
import app.cash.sqldelight.dialects.mysql.MySqlDialect
import app.cash.sqldelight.dialects.postgresql.PostgreSqlDialect
import app.cash.sqldelight.dialects.sqlite_3_18.SqliteDialect

enum class TestDialect(val dialect: SqlDelightDialect) {
  SQLITE_3_18(SqliteDialect()),
  SQLITE_3_24(app.cash.sqldelight.dialects.sqlite_3_24.SqliteDialect()),
  SQLITE_3_25(app.cash.sqldelight.dialects.sqlite_3_25.SqliteDialect()),
  SQLITE_3_30(app.cash.sqldelight.dialects.sqlite_3_30.SqliteDialect()),
  POSTGRESQL(PostgreSqlDialect()),
  HSQL(HsqlDialect()),
  MYSQL(MySqlDialect()),
}
