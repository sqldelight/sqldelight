package com.example.sqldelight.hockey

import kotlin.test.Test
import kotlin.test.assertTrue

class JvmSchemaTest {

  @Test
  fun teamsCreated() = testing { db ->
    val teams = db.teamQueries.selectAll().executeAsList()
    assertTrue(
      teams.any {
        it.name == "Anaheim Ducks"
      },
    )
  }

  @Test
  fun playersCreated() = testing { db ->
    val players = db.playerQueries.selectAll().executeAsList()
    assertTrue(
      players.any {
        it.last_name == "Karlsson"
      },
    )
  }
}
