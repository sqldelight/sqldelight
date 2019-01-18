package com.example.sqldelight.hockey.data

import com.example.sqldelight.hockey.HockeyDb
import com.example.sqldelight.hockey.data.AndroidDb.driver
import com.squareup.sqldelight.db.SqlDriver
import java.lang.IllegalStateException

object AndroidDb{
  lateinit var driver: SqlDriver
}

actual fun createDb(): HockeyDb = createQueryWrapper(driver)