package com.example.sqldelight.hockey.data

import com.example.sqldelight.hockey.HockeyDb
import com.squareup.sqldelight.db.SqlDriver

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
