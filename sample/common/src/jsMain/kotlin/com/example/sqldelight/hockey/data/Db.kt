package com.example.sqldelight.hockey.data

import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import com.example.sqldelight.hockey.HockeyDb

object Db {

  private lateinit var driver: SqlDriver<SqlPreparedStatement, SqlCursor>
  private lateinit var db: HockeyDb

  fun dbSetup(driver: SqlDriver<SqlPreparedStatement, SqlCursor>) {
    this.driver = driver
    this.db = createQueryWrapper(driver)
  }

  internal fun dbClear() {
    driver.close()
  }

  val instance: HockeyDb
    get() = db
}
