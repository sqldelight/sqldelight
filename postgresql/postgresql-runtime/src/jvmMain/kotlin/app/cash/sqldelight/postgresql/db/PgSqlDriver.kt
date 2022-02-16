package app.cash.sqldelight.postgresql.db

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.SqlDriver

actual class PgSqlDriver : SqlDriver<PgSqlPreparedStatement, PgSqlCursor> {

  actual override fun execute(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (PgSqlPreparedStatement.() -> Unit)?
  ) {
    TODO("Not yet implemented")
  }

  actual override fun executeQuery(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (PgSqlPreparedStatement.() -> Unit)?
  ): PgSqlCursor {
    TODO("Not yet implemented")
  }

  actual override fun close() {
    TODO("Not yet implemented")
  }

  actual override fun newTransaction(): Transacter.Transaction {
    TODO("Not yet implemented")
  }

  actual override fun currentTransaction(): Transacter.Transaction? {
    TODO("Not yet implemented")
  }

  actual override fun addListener(listener: Query.Listener, queryKeys: Array<String>) {
    TODO("Not yet implemented")
  }

  actual override fun removeListener(listener: Query.Listener, queryKeys: Array<String>) {
    TODO("Not yet implemented")
  }

  actual override fun notifyListeners(queryKeys: Array<String>) {
    TODO("Not yet implemented")
  }
}