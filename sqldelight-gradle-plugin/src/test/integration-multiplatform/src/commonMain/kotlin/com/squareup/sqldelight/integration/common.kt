package com.squareup.sqldelight.integration

import com.squareup.sqldelight.db.SqlDriver

expect fun createSqlDatabase(): SqlDriver

// TODO: Replace these with stately primitives?
expect class MPWorker(){
  fun <T> runBackground(backJob:()->T):MPFuture<T>
  fun requestTermination()
}

expect class MPFuture<T>{
  fun consume():T
}

fun doThingsWithGeneratedCode() {
  val stuff = Person.Impl(10, "Jesse", "Wilson")
}
