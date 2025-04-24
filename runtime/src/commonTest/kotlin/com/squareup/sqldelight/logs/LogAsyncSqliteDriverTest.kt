package com.squareup.sqldelight.logs

import app.cash.sqldelight.Query
import app.cash.sqldelight.SuspendingTransacterImpl
import app.cash.sqldelight.Transacter.Transaction
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.logs.LogSqliteDriver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class LogAsyncSqliteDriverTest {

  private lateinit var driver: LogSqliteDriver
  private lateinit var transacter: SuspendingTransacterImpl
  private val logs = mutableListOf<String>()

  @BeforeTest fun setup() {
    driver = LogSqliteDriver(FakeAsyncSqlDriver()) { log ->
      logs.add(log)
    }
    transacter = object : SuspendingTransacterImpl(driver) {}
  }

  @AfterTest fun tearDown() {
    driver.close()
    logs.clear()
  }

  @Test
  fun insertLogsAreCorrect() {
    val insert = { binders: SqlPreparedStatement.() -> Unit ->
      driver.execute(2, "INSERT INTO test VALUES (?, ?);", 2, binders)
    }

    insert {
      bindLong(1, 1)
      bindString(2, "Alec")
    }

    insert {}

    assertEquals("EXECUTE\n INSERT INTO test VALUES (?, ?);", logs[0])
    assertEquals(" [1, Alec]", logs[1])
    assertEquals("EXECUTE\n INSERT INTO test VALUES (?, ?);", logs[2])
  }

  @Test
  fun queryLogsAreCorrect() {
    val query = {
      driver.executeQuery(3, "SELECT * FROM test", { QueryResult.Unit }, 0)
    }

    query()

    assertEquals("QUERY\n SELECT * FROM test", logs[0])
  }

  @Test
  fun transactionLogsAreCorrect() = runTest {
    transacter.transaction {}
    transacter.transaction { rollback() }
    transacter.transaction {
      val insert = { binders: SqlPreparedStatement.() -> Unit ->
        driver.execute(2, "INSERT INTO test VALUES (?, ?);", 2, binders)
      }

      insert {
        bindLong(1, 1)
        bindString(2, "Alec")
      }
    }

    assertEquals("TRANSACTION BEGIN", logs[0])
    assertEquals("TRANSACTION COMMIT", logs[1])
    assertEquals("TRANSACTION BEGIN", logs[2])
    assertEquals("TRANSACTION ROLLBACK", logs[3])
    assertEquals("TRANSACTION BEGIN", logs[4])
    assertEquals("EXECUTE\n INSERT INTO test VALUES (?, ?);", logs[5])
    assertEquals(" [1, Alec]", logs[6])
    assertEquals("TRANSACTION COMMIT", logs[7])
  }
}

class FakeAsyncSqlDriver : SqlDriver {
  override fun <R> executeQuery(
    identifier: Int?,
    sql: String,
    mapper: (SqlCursor) -> QueryResult<R>,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?,
  ): QueryResult<R> {
    return QueryResult.AsyncValue {
      mapper(FakeSqlCursor()).value
    }
  }

  override fun execute(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?,
  ): QueryResult<Long> {
    return QueryResult.AsyncValue { 0 }
  }

  override fun newTransaction(): QueryResult<Transaction> {
    return QueryResult.AsyncValue { FakeTransaction() }
  }

  override fun currentTransaction(): Transaction? {
    return null
  }

  override fun addListener(vararg queryKeys: String, listener: Query.Listener) {
  }

  override fun removeListener(vararg queryKeys: String, listener: Query.Listener) {
  }

  override fun notifyListeners(vararg queryKeys: String) {
  }

  override fun close() {
  }
}
