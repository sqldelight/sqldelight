package com.example.sqldelight.hockey.data

import com.example.sqldelight.hockey.HockeyDb
import com.squareup.sqldelight.db.SqlDriver

object JvmDb{
  var driver: SqlDriver? = null
}

actual fun createDb(): HockeyDb = createQueryWrapper(JvmDb.driver!!)