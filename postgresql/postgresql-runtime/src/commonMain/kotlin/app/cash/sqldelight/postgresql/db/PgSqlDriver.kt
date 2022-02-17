package app.cash.sqldelight.postgresql.db

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.SqlDriver

class PgSqlDriver : SqlDriver<PgSqlPreparedStatement, PgSqlCursor> {

  override fun execute(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (PgSqlPreparedStatement.() -> Unit)?
  ) {
    TODO("Not yet implemented")
  }

  override fun executeQuery(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (PgSqlPreparedStatement.() -> Unit)?
  ): PgSqlCursor {
    TODO("Not yet implemented")
  }

  override fun newTransaction(): Transacter.Transaction {
    TODO()
  }

  override fun currentTransaction(): Transacter.Transaction? {
    TODO()
  }

  override fun addListener(listener: Query.Listener, queryKeys: Array<String>) {
    TODO()
  }

  override fun removeListener(listener: Query.Listener, queryKeys: Array<String>) {
    TODO()
  }

  override fun notifyListeners(queryKeys: Array<String>) {
    TODO()
  }

  override fun close() {
    TODO()
  }
}
