package com.example.sqldelight.hockey.data

import com.example.sqldelight.hockey.HockeyDb
import com.squareup.sqldelight.drivers.sqljs.JsSqlDriver
import com.squareup.sqldelight.drivers.sqljs.initSql
import com.squareup.sqldelight.drivers.sqljs.invoke

suspend fun Db.getInstance(): HockeyDb {
    if (!Db.ready) {
        val sql = initSql()
        val db = sql.Database()
        val driver = JsSqlDriver(db)
        Schema.create(driver)
        Db.dbSetup(driver)
    }
    return Db.instance
}
