package com.example.sqldelight.hockey.ui

import com.example.sqldelight.hockey.data.Db

//For Swift
object TeamData {
  fun teams() = Db.instance.teamQueries.selectAll().executeAsList()
}