package com.example.sqldelight.hockey

import android.app.Application
import com.example.sqldelight.hockey.data.AndroidDb
import com.example.sqldelight.hockey.data.Schema
import com.squareup.sqldelight.android.AndroidSqliteDriver

class HockeyApp : Application() {
  override fun onCreate() {
    super.onCreate()
    AndroidDb.driver = AndroidSqliteDriver(Schema, this)
  }
}