package com.example.sqldelight.hockey

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.sqldelight.hockey.data.Db
import com.example.sqldelight.hockey.data.Schema

actual fun createDriver() {
  val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
  Schema.create(driver)
  Db.dbSetup(driver)
}

actual fun closeDriver() {
  Db.dbClear()
}

actual fun BaseTest.getDb(): HockeyDb = Db.instance
