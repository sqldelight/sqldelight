package com.example.sqldelight.hockey.data

import android.content.Context
import com.example.sqldelight.hockey.HockeyDb
import com.squareup.sqldelight.android.AndroidSqliteDriver

fun Db.getInstance(context: Context): HockeyDb {
  if (!Db.ready) {
    Db.dbSetup(AndroidSqliteDriver(Schema, context))
  }
  return Db.instance
}
