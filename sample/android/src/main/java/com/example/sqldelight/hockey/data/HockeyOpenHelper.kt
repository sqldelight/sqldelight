package com.example.sqldelight.hockey.data

import android.content.Context
import com.example.sqldelight.hockey.HockeyDb
import com.squareup.sqldelight.android.AndroidSqliteDriver

object Db {
  private var instance: HockeyDb? = null

  fun getInstance(context: Context): HockeyDb {
    instance?.let { return it }

    return createQueryWrapper(AndroidSqliteDriver(Schema, context)).also {
      instance = it
    }
  }
}
