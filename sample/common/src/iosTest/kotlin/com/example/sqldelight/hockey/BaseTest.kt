package com.example.sqldelight.hockey

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.sqldelight.hockey.data.Db
import com.example.sqldelight.hockey.data.Schema

actual suspend fun createDriver() {
  Db.dbSetup(
    NativeSqliteDriver(Schema, "sampledb", onConfiguration = {
      it.copy(
        inMemory = true,
      )
    }),
  )
}

actual suspend fun closeDriver() {
  Db.dbClear()
}

actual fun getDb(): HockeyDb = Db.instance
