package com.example.sqldelight.hockey.ui

import com.example.sqldelight.hockey.data.Db

//For Swift
@Suppress("unused")
object PlayerData {
  fun players(teamId: Long) = Db.instance.playerQueries.forTeam(teamId).executeAsList()
}