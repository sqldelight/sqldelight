package com.example.sqldelight.hockey

import com.example.sqldelight.hockey.data.Db

actual fun createDriver() {
    TODO("Async common tests not implemented")
}

actual fun closeDriver() {
    TODO("Async common tests not implemented")
}

actual fun BaseTest.getDb(): HockeyDb = Db.instance
