package com.example.sqldelight.hockey

import app.cash.sqldelight.async.coroutines.awaitAsList
import kotlin.test.Test
import kotlin.test.assertTrue

class HockeyDbTest {
  @Test
  fun teamsCreated() = testDb { db ->
    val teams = db.teamQueries.selectAll().awaitAsList()
    assertTrue(
      teams.any {
        it.name == "Anaheim Ducks"
      },
    )
  }

  @Test
  fun playersCreated() = testDb { db ->
    val players = db.playerQueries.selectAll().awaitAsList()
    assertTrue(
      players.any {
        it.last_name == "Karlsson"
      },
    )
  }
}
