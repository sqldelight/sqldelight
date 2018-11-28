package com.squareup.sqldelight.integration

import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.drivers.ios.SQLiterHelper
import co.touchlab.sqliter.createDatabaseManager
import co.touchlab.sqliter.deleteDatabase
import co.touchlab.sqliter.DatabaseConfiguration
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze
import kotlin.system.getTimeMillis

actual fun createSqlDatabase(): SqlDatabase {
  val configuration = DatabaseConfiguration("testdb", 1, { })
  deleteDatabase(configuration.name)
  return SQLiterHelper(createDatabaseManager(configuration))
}

actual class MPWorker actual constructor(){
  val worker = Worker.start()
  actual fun <T> runBackground(backJob: () -> T): MPFuture<T> {
    return MPFuture(worker.execute(TransferMode.SAFE, {backJob.freeze()}){
      it()
    })
  }

  actual fun requestTermination() {
    worker.requestTermination().result
  }
}

actual class MPFuture<T>(private val future:Future<T>) {
  actual fun consume():T = future.result
}
