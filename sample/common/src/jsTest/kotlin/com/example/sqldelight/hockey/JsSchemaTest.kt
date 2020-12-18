package com.example.sqldelight.hockey

import kotlin.test.Test
import kotlin.test.assertTrue

class JsSchemaTest : BaseTest() {

  @Test
  fun teamsCreated() = dbPromise.then {
    val teams = getDb().teamQueries.selectAll().executeAsList()
    assertTrue(
      teams.any {
        it.name == "Anaheim Ducks"
      }
    )
  }

  @Test
  fun playersCreated() = dbPromise.then {
    val players = getDb().playerQueries.selectAll().executeAsList()
    assertTrue(
      players.any {
        it.last_name == "Karlsson"
      }
    )
  }
}
