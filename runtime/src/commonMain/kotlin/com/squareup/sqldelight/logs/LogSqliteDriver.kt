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
package com.squareup.sqldelight.logs

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.SqlPreparedStatement

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
  ) {
    logger("EXECUTE\n $sql")
    logParameters(binders)
    sqlDriver.execute(identifier, sql, parameters, binders)
  }

  override fun executeQuery(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?
  ): SqlCursor {
    logger("QUERY\n $sql")
    logParameters(binders)
    return sqlDriver.executeQuery(identifier, sql, parameters, binders)
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

  override fun addListener(listener: Query.Listener, vararg queryKeys: String) {
    logger("BEGIN $listener LISTENING TO [${queryKeys.joinToString()}]")
    sqlDriver.addListener(listener, *queryKeys)
  }

  override fun removeListener(listener: Query.Listener, vararg queryKeys: String) {
    logger("END $listener LISTENING TO [${queryKeys.joinToString()}]")
    sqlDriver.removeListener(listener, *queryKeys)
  }

  override fun notifyListeners(vararg queryKeys: String) {
    logger("NOTIFYING LISTENERS OF [${queryKeys.joinToString()}]")
    sqlDriver.notifyListeners(*queryKeys)
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

  fun getAndClearParameters(): List<Any?> {
    val list = values.toList()
    values.clear()
    return list
  }
}
