package com.example.sqldelight.hockey

import app.cash.sqldelight.driver.sqljs.initSqlDriver
import com.example.sqldelight.hockey.data.Db
import com.example.sqldelight.hockey.data.Schema
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.await

actual suspend fun createDriver() = coroutineScope {
  val driver = initSqlDriver(Schema).await()
  Db.dbSetup(driver)
}

actual suspend fun closeDriver() = coroutineScope {
  Db.dbClear()
}

actual fun getDb(): HockeyDb = Db.instance
