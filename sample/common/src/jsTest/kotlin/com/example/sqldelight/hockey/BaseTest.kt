package com.example.sqldelight.hockey

import app.cash.sqldelight.driver.sqljs.initSqlDriver
import com.example.sqldelight.hockey.data.Db
import com.example.sqldelight.hockey.data.Schema
import kotlinx.coroutines.await

actual suspend fun createDriver() {
  val driver = initSqlDriver(Schema).await()
  Db.dbSetup(driver)
}

actual suspend fun closeDriver() {
  Db.dbClear()
}

actual fun getDb(): HockeyDb = Db.instance
