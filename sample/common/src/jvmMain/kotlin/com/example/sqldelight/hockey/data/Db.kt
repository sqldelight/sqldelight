package com.example.sqldelight.hockey.data

import app.cash.sqldelight.db.SqlDriver
import com.example.sqldelight.hockey.HockeyDb

object Db {
  private var driverRef: SqlDriver? = null
  private var dbRef: HockeyDb? = null

  val ready: Boolean
    get() = driverRef != null

  fun dbSetup(driver: SqlDriver) {
    val db = createQueryWrapper(driver)
    driverRef = driver
    dbRef = db
  }

  internal fun dbClear() {
    driverRef!!.close()
    dbRef = null
    driverRef = null
  }

  val instance: HockeyDb
    get() = dbRef!!
}
