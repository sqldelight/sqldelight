package com.example.sqldelight.hockey.data

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.sqldelight.hockey.HockeyDb

fun Db.getInstance(context: Context): HockeyDb {
  if (!Db.ready) {
    Db.dbSetup(AndroidSqliteDriver(Schema, context))
  }
  return Db.instance
}
