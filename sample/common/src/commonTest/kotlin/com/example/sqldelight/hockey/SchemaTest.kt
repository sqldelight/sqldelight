package com.example.sqldelight.hockey

import kotlin.test.Test
import kotlin.test.assertTrue

class SchemaTest : BaseTest() {
  @Test
  fun teamsCreated() {
    val teams = getDb().teamQueries.selectAll().executeAsList()
    assertTrue(teams.any {
      it.name == "Anaheim Ducks"
    })
  }

  @Test
  fun playersCreated() {
    val players = getDb().playerQueries.selectAll().executeAsList()
    assertTrue(players.any {
      it.last_name == "Karlsson"
    })
  }
}