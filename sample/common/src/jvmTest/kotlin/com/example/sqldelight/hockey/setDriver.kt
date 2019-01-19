package com.example.sqldelight.hockey

import com.example.sqldelight.hockey.data.JvmDb
import com.example.sqldelight.hockey.data.Schema
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver

actual fun setDriver() {
  val driver = JdbcSqliteDriver()
  Schema.create(driver)
  JvmDb.driver = driver
}