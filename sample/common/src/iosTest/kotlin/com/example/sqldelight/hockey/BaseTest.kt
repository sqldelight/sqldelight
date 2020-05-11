package com.example.sqldelight.hockey

import com.example.sqldelight.hockey.data.Db
import com.example.sqldelight.hockey.data.Schema
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver

actual fun createDriver() {
  Db.dbSetup(NativeSqliteDriver(Schema, "sampledb"))
}

actual fun closeDriver() {
  Db.dbClear()
}

actual fun BaseTest.getDb(): HockeyDb = Db.instance
