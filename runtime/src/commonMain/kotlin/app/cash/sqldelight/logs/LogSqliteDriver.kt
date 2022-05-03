/*
 * Copyright (C) 2018 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.sqldelight.logs

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement

class LogSqliteDriver(
  private val sqlDriver: SqlDriver,
  private val logger: (String) -> Unit
) : SqlDriver {

  override fun currentTransaction(): Transacter.Transaction? {
    return sqlDriver.currentTransaction()
  }

  override fun execute(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?
  ): Long {
    logger("EXECUTE\n $sql")
    logParameters(binders)
    return sqlDriver.execute(identifier, sql, parameters, binders)
  }

  override fun <R> executeQuery(
    identifier: Int?,
    sql: String,
    mapper: (SqlCursor) -> R,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?
  ): R {
    logger("QUERY\n $sql")
    logParameters(binders)
    return sqlDriver.executeQuery(identifier, sql, mapper, parameters, binders)
  }

  override fun newTransaction(): Transacter.Transaction {
    logger("TRANSACTION BEGIN")
    val transaction = sqlDriver.newTransaction()
    transaction.afterCommit { logger("TRANSACTION COMMIT") }
    transaction.afterRollback { logger("TRANSACTION ROLLBACK") }
    return transaction
  }

  override fun close() {
    logger("CLOSE CONNECTION")
    sqlDriver.close()
  }

  override fun addListener(listener: Query.Listener, queryKeys: Array<String>) {
    logger("BEGIN $listener LISTENING TO [${queryKeys.joinToString()}]")
    sqlDriver.addListener(listener, queryKeys)
  }

  override fun removeListener(listener: Query.Listener, queryKeys: Array<String>) {
    logger("END $listener LISTENING TO [${queryKeys.joinToString()}]")
    sqlDriver.removeListener(listener, queryKeys)
  }

  override fun notifyListeners(queryKeys: Array<String>) {
    logger("NOTIFYING LISTENERS OF [${queryKeys.joinToString()}]")
    sqlDriver.notifyListeners(queryKeys)
  }

  private fun logParameters(binders: (SqlPreparedStatement.() -> Unit)?) {
    binders?.let { func ->
      val parametersInterceptor = StatementParameterInterceptor()
      parametersInterceptor.func()
      val logParameters = parametersInterceptor.getAndClearParameters()
      if (logParameters.isNotEmpty()) logger(" $logParameters")
    }
  }
}

class StatementParameterInterceptor : SqlPreparedStatement {
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
