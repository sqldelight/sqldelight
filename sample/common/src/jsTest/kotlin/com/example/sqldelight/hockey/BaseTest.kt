package com.example.sqldelight.hockey

import com.example.sqldelight.hockey.data.Db
import com.example.sqldelight.hockey.data.Schema
import com.squareup.sqldelight.drivers.sqljs.initSqlDriver
import kotlin.js.Promise

lateinit var dbPromise: Promise<Unit>

actual fun createDriver() {
    dbPromise = initSqlDriver(Schema).then { Db.dbSetup(it) }
}

actual fun closeDriver() {
    dbPromise.then { Db.dbClear() }
}

actual fun BaseTest.getDb(): HockeyDb = Db.instance
