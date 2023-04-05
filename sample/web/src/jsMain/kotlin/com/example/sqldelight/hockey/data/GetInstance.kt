package com.example.sqldelight.hockey.data

import app.cash.sqldelight.driver.sqljs.initSqlDriver
import com.example.sqldelight.hockey.HockeyDb
import kotlin.js.Promise

fun Db.getInstance(): Promise<HockeyDb> = initSqlDriver(Schema).then {
  Db.dbSetup(it)
  Db.instance
}
