package com.example.sqldelight.hockey.data

import com.example.sqldelight.hockey.HockeyDb
import com.squareup.sqldelight.db.SqlDriver
import java.lang.IllegalStateException

var driver: SqlDriver? = null

actual fun createDb(): HockeyDb {
  driver?.let {
    return createQueryWrapper(it)
  }

  throw IllegalStateException("Driver must be set")
}