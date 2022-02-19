package com.example.sqldelight.hockey.data

import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import com.example.sqldelight.hockey.HockeyDb

object Db {
  private var driverRef: SqlDriver<SqlPreparedStatement, SqlCursor>? = null
  private var dbRef: HockeyDb? = null

  val ready: Boolean
    get() = driverRef != null

  fun dbSetup(driver: SqlDriver<SqlPreparedStatement, SqlCursor>) {
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
