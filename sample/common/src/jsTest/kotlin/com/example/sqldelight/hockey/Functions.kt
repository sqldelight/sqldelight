package com.example.sqldelight.hockey

import com.example.sqldelight.hockey.data.Db
import com.example.sqldelight.hockey.data.Schema
import com.squareup.sqldelight.drivers.sqljs.JsSqlDriver
import com.squareup.sqldelight.drivers.sqljs.invoke
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

actual fun createDriver() {
    TODO("Async common tests not implemented")
}

suspend fun initDriver() {
    //val sql = initSql()
    //val db = sql.Database()
    //val driver = JsSqlDriver(db)
    //Schema.create(driver)
    //Db.dbSetup(driver)
}

actual fun closeDriver() {
  Db.dbClear()
}

actual fun BaseTest.getDb(): HockeyDb = Db.instance
