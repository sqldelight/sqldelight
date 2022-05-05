package app.cash.sqldelight.async.logs

import app.cash.sqldelight.async.AsyncQuery
import app.cash.sqldelight.async.AsyncTransacter
import app.cash.sqldelight.async.db.AsyncSqlCursor
import app.cash.sqldelight.async.db.AsyncSqlDriver
import app.cash.sqldelight.async.db.AsyncSqlPreparedStatement

class LogAsyncSqlDriver(
  private val sqlDriver: AsyncSqlDriver,
  private val logger: (String) -> Unit
) : AsyncSqlDriver {
  override suspend fun <R> executeQuery(identifier: Int?, sql: String, mapper: (AsyncSqlCursor) -> R, parameters: Int, binders: (AsyncSqlPreparedStatement.() -> Unit)?): R {
    logger("QUERY\n $sql")
    logParameters(binders)
    return sqlDriver.executeQuery(identifier, sql, mapper, parameters, binders)
  }

  override suspend fun execute(identifier: Int?, sql: String, parameters: Int, binders: (AsyncSqlPreparedStatement.() -> Unit)?): Long {
    logger("EXECUTE\n $sql")
    logParameters(binders)
    return sqlDriver.execute(identifier, sql, parameters, binders)
  }

  override suspend fun newTransaction(): AsyncTransacter.Transaction {
    logger("TRANSACTION BEGIN")
    val transaction = sqlDriver.newTransaction()
    transaction.afterCommit { logger("TRANSACTION COMMIT") }
    transaction.afterRollback { logger("TRANSACTION ROLLBACK") }
    return transaction
  }

  override fun currentTransaction(): AsyncTransacter.Transaction? {
    return sqlDriver.currentTransaction()
  }

  override fun addListener(listener: AsyncQuery.Listener, queryKeys: Array<String>) {
    logger("BEGIN $listener LISTENING TO [${queryKeys.joinToString()}]")
    sqlDriver.addListener(listener, queryKeys)
  }

  override fun removeListener(listener: AsyncQuery.Listener, queryKeys: Array<String>) {
    logger("END $listener LISTENING TO [${queryKeys.joinToString()}]")
    sqlDriver.removeListener(listener, queryKeys)
  }

  override fun notifyListeners(queryKeys: Array<String>) {
    logger("NOTIFYING LISTENERS OF [${queryKeys.joinToString()}]")
    sqlDriver.notifyListeners(queryKeys)
  }

  override suspend fun close() {
    logger("CLOSE CONNECTION")
    sqlDriver.close()
  }

  private fun logParameters(binders: (AsyncSqlPreparedStatement.() -> Unit)?) {
    binders?.let { func ->
      val parametersInterceptor = StatementParameterInterceptor()
      parametersInterceptor.func()
      val logParameters = parametersInterceptor.getAndClearParameters()
      if (logParameters.isNotEmpty()) logger(" $logParameters")
    }
  }
}

class StatementParameterInterceptor : AsyncSqlPreparedStatement {
  private val values = mutableListOf<Any?>()

  override fun bindBytes(
    index: Int,
    bytes: ByteArray?
  ) {
    values.add(bytes)
  }

  override fun bindDouble(
    index: Int,
    double: Double?
  ) {
    values.add(double)
  }

  override fun bindLong(
    index: Int,
    long: Long?
  ) {
    values.add(long)
  }

  override fun bindString(
    index: Int,
    string: String?
  ) {
    values.add(string)
  }

  override fun bindBoolean(
    index: Int,
    boolean: Boolean?
  ) {
    values.add(boolean)
  }

  fun getAndClearParameters(): List<Any?> {
    val list = values.toList()
    values.clear()
    return list
  }
}
