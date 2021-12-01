package com.example.sqldelight.hockey

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.sqldelight.hockey.data.Db
import com.example.sqldelight.hockey.data.Schema

actual fun createDriver() {
  Db.dbSetup(NativeSqliteDriver(Schema, "sampledb"))
}

actual fun closeDriver() {
  Db.dbClear()
}

actual fun BaseTest.getDb(): HockeyDb = Db.instance
