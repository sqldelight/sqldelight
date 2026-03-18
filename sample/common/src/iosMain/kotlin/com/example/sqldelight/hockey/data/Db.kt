package com.example.sqldelight.hockey.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.sqldelight.hockey.HockeyDb
import kotlin.concurrent.AtomicReference

object Db {
  private val driverRef = AtomicReference<SqlDriver?>(null)
  private val dbRef = AtomicReference<HockeyDb?>(null)

  internal fun dbSetup(driver: SqlDriver) {
    val db = createQueryWrapper(driver)
    driverRef.value = driver
    dbRef.value = db
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
