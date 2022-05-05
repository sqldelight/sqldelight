package com.squareup.sqldelight.driver.test

import app.cash.sqldelight.async.db.AsyncSqlDriver

abstract class AsyncTestBase {
  abstract suspend fun setupDatabase(schema: AsyncSqlDriver.Schema): AsyncSqlDriver

  abstract suspend fun setup()

  abstract suspend fun teardown()

  fun runTest(block: suspend () -> Unit) = kotlinx.coroutines.test.runTest {
    setup()
    block()
    teardown()
  }
}
