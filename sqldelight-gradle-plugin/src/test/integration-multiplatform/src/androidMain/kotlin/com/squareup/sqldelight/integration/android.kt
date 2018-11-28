package com.squareup.sqldelight.integration

import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.sqlite.driver.SqliteJdbcOpenHelper
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

actual fun createSqlDatabase(): SqlDatabase {
  return SqliteJdbcOpenHelper().apply {
    QueryWrapper.Schema.create(getConnection())
  }
}

actual class MPWorker actual constructor(){
  private val executor = Executors.newSingleThreadExecutor()
  actual fun <T> runBackground(backJob: () -> T): MPFuture<T> {
    return MPFuture(executor.submit(backJob) as Future<T>)
  }

  actual fun requestTermination() {
    executor.shutdown()
    executor.awaitTermination(30, TimeUnit.SECONDS)
  }
}

actual class MPFuture<T>(private val future:Future<T>) {
  actual fun consume():T = future.get()
}
