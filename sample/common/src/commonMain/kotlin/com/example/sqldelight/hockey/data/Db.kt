package com.example.sqldelight.hockey.data

import com.example.sqldelight.hockey.HockeyDb

object Db {
    val instance:HockeyDb by lazy {
        createDb()
    }
}

expect fun createDb(): HockeyDb
