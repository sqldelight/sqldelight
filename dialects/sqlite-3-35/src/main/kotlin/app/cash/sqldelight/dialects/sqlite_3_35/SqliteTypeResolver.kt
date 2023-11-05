package app.cash.sqldelight.dialects.sqlite_3_35

import app.cash.sqldelight.dialect.api.QueryWithResults
import app.cash.sqldelight.dialect.api.ReturningQueryable
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.sqlite_3_24.SqliteTypeResolver as Sqlite324TypeResolver
import app.cash.sqldelight.dialects.sqlite_3_35.grammar.psi.SqliteDeleteStmtLimited
import app.cash.sqldelight.dialects.sqlite_3_35.grammar.psi.SqliteInsertStmt
import app.cash.sqldelight.dialects.sqlite_3_35.grammar.psi.SqliteUpdateStmtLimited
import com.alecstrong.sql.psi.core.psi.SqlStmt

class SqliteTypeResolver(private val parentResolver: TypeResolver) : Sqlite324TypeResolver(parentResolver) {
  override fun queryWithResults(sqlStmt: SqlStmt): QueryWithResults? {
    sqlStmt.insertStmt?.let { insert ->
      check(insert is SqliteInsertStmt)
      insert.returningClause?.let { return ReturningQueryable(insert, it, insert.tableName) }
    }
    sqlStmt.updateStmtLimited?.let { update ->
      check(update is SqliteUpdateStmtLimited)
      update.returningClause?.let { return ReturningQueryable(update, it, update.qualifiedTableName.tableName) }
    }
    sqlStmt.deleteStmtLimited?.let { delete ->
      check(delete is SqliteDeleteStmtLimited)
      delete.returningClause?.let { return ReturningQueryable(delete, it, delete.qualifiedTableName?.tableName) }
    }
    return parentResolver.queryWithResults(sqlStmt)
  }
}
