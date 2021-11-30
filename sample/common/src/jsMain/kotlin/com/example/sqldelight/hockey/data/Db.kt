package com.example.sqldelight.hockey.data

import app.cash.sqldelight.db.SqlDriver
import com.example.sqldelight.hockey.HockeyDb

object Db {

  private lateinit var driver: SqlDriver
  private lateinit var db: HockeyDb

  fun dbSetup(driver: SqlDriver) {
    this.driver = driver
    this.db = createQueryWrapper(driver)
  }

  internal fun dbClear() {
    driver.close()
  }

  val instance: HockeyDb
    get() = db
}
