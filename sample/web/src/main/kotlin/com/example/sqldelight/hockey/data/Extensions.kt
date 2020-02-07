package com.example.sqldelight.hockey.data

import com.example.sqldelight.hockey.HockeyDb
import com.squareup.sqldelight.drivers.sqljs.initSqlDriver
import kotlin.js.Promise

fun Db.getInstance(): Promise<HockeyDb> = initSqlDriver(Schema).then {
    Db.dbSetup(it)
    Db.instance
}
