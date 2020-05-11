package com.example.sqldelight.hockey.data

import com.example.sqldelight.hockey.HockeyDb
import com.squareup.sqldelight.db.SqlDriver

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
