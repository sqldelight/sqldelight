package app.cash.sqldelight.async.coroutines

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import kotlinx.coroutines.runBlocking

fun SqlSchema<QueryResult.AsyncValue<Unit>>.synchronous() = object : SqlSchema<QueryResult.Value<Unit>> {
  override val version = this@synchronous.version

  override fun create(driver: SqlDriver) = QueryResult.Value(
    runBlocking {
      this@synchronous.create(driver).await()
    },
  )

  override fun migrate(
    driver: SqlDriver,
    oldVersion: Long,
    newVersion: Long,
    vararg callbacks: AfterVersion,
  ) = QueryResult.Value(
    runBlocking { this@synchronous.migrate(driver, oldVersion, newVersion, *callbacks).await() },
  )
}
