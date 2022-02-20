package app.cash.sqldelight.integration

import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement


expect fun createSqlDatabase(): SqlDriver<SqlPreparedStatement, SqlCursor>

// TODO: Replace these with stately primitives?
expect class MPWorker() {
  fun <T> runBackground(backJob: () -> T): MPFuture<T>
  fun requestTermination()
}

expect class MPFuture<T> {
  fun consume(): T
}

fun doThingsWithGeneratedCode() {
  val stuff = Person(10, "Jesse", "Wilson")
}
