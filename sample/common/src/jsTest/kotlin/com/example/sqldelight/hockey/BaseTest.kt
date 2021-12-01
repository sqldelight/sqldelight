package com.example.sqldelight.hockey

import app.cash.sqldelight.driver.sqljs.initSqlDriver
import com.example.sqldelight.hockey.data.Db
import com.example.sqldelight.hockey.data.Schema
import kotlin.js.Promise

lateinit var dbPromise: Promise<Unit>

actual fun createDriver() {
  dbPromise = initSqlDriver(Schema).then { Db.dbSetup(it) }
}

actual fun closeDriver() {
  dbPromise.then { Db.dbClear() }
}

actual fun BaseTest.getDb(): HockeyDb = Db.instance
