package com.squareup.sqldelight.integration

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.Companion.IN_MEMORY
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

actual fun createSqlDatabase(): SqlDriver {
  return JdbcSqliteDriver(IN_MEMORY).apply {
    QueryWrapper.Schema.create(this)
  }
}

actual class MPWorker actual constructor() {
  private val executor = Executors.newSingleThreadExecutor()
  actual fun <T> runBackground(backJob: () -> T): MPFuture<T> {
    return MPFuture(executor.submit(backJob) as Future<T>)
  }

  actual fun requestTermination() {
    executor.shutdown()
    executor.awaitTermination(30, TimeUnit.SECONDS)
  }
}

actual class MPFuture<T>(private val future: Future<T>) {
  actual fun consume(): T = future.get()
}
