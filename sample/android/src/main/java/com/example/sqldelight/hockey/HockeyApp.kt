package com.example.sqldelight.hockey

import android.app.Application
import com.example.sqldelight.hockey.data.Schema
import com.example.sqldelight.hockey.data.driver
import com.squareup.sqldelight.android.AndroidSqliteDriver

class HockeyApp:Application(){
    override fun onCreate() {
        super.onCreate()
        driver = AndroidSqliteDriver(Schema, this)
    }
}