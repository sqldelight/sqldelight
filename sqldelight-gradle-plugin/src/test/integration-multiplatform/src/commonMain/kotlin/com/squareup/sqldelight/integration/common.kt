package com.squareup.sqldelight.integration

import com.squareup.sqldelight.db.SqlDatabase

expect fun createSqlDatabase(): SqlDatabase

// TODO: Replace these with stately primitives?
expect class MPWorker(){
  fun <T> runBackground(backJob:()->T):MPFuture<T>
  fun requestTermination()
}

expect class MPFuture<T>{
  fun consume():T
}
