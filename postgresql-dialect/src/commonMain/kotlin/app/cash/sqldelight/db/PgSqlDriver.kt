package app.cash.sqldelight.db

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter

expect class PgSqlDriver : SqlDriver<PgSqlPreparedStatement, PgSqlCursor> {

  override fun execute(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (PgSqlPreparedStatement.() -> Unit)?
  )

  override fun executeQuery(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (PgSqlPreparedStatement.() -> Unit)?
  ): PgSqlCursor

  override fun newTransaction(): Transacter.Transaction

  override fun currentTransaction(): Transacter.Transaction?

  override fun addListener(listener: Query.Listener, queryKeys: Array<String>)

  override fun removeListener(listener: Query.Listener, queryKeys: Array<String>)

  override fun notifyListeners(queryKeys: Array<String>)

  override fun close()
}
