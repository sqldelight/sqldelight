package app.cash.sqldelight.dialects.sqlite_3_35

import app.cash.sqldelight.dialect.api.QueryWithResults
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.sqlite_3_35.grammar.psi.SqliteInsertStmt
import com.alecstrong.sql.psi.core.psi.SqlStmt
import app.cash.sqldelight.dialects.sqlite_3_24.SqliteTypeResolver as Sqlite324TypeResolver

class SqliteTypeResolver(private val parentResolver: TypeResolver) : Sqlite324TypeResolver(parentResolver) {
  override fun queryWithResults(sqlStmt: SqlStmt): QueryWithResults? {
    sqlStmt.insertStmt?.let { insert ->
      check(insert is SqliteInsertStmt)
      insert.returningClause?.let {
        return object : QueryWithResults {
          override val statement = insert
          override val select = it
          override val pureTable = insert.tableName
        }
      }
    }
    return parentResolver.queryWithResults(sqlStmt)
  }
}
