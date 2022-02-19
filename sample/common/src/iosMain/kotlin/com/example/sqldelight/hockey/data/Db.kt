package com.example.sqldelight.hockey.data

import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.sqldelight.hockey.HockeyDb
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze

object Db {
  private val driverRef = AtomicReference<SqlDriver<SqlPreparedStatement, SqlCursor>?>(null)
  private val dbRef = AtomicReference<HockeyDb?>(null)

  internal fun dbSetup(driver: SqlDriver<SqlPreparedStatement, SqlCursor>) {
    val db = createQueryWrapper(driver)
    driverRef.value = driver.freeze()
    dbRef.value = db.freeze()
  }

  internal fun dbClear() {
    driverRef.value!!.close()
    dbRef.value = null
    driverRef.value = null
  }

  // Called from Swift
  @Suppress("unused")
  fun defaultDriver() {
    Db.dbSetup(NativeSqliteDriver(Schema, "sampledb"))
  }

  val instance: HockeyDb
    get() = dbRef.value!!
}
