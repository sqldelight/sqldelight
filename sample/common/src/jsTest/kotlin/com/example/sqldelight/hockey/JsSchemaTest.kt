package com.example.sqldelight.hockey

import com.example.sqldelight.hockey.data.Db
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlin.test.Test
import kotlin.test.assertTrue

class JsSchemaTest : CoroutineScope by GlobalScope {

  /*
  @Test
  fun teamsCreated() = runTest {
    initDriver()
    val teams = getDb().teamQueries.selectAll().executeAsList()
    assertTrue(teams.any {
      it.name == "Anaheim Ducks"
    })
    closeDriver()
  }

  @Test
  fun playersCreated() = runTest {
    initDriver()
    val players = getDb().playerQueries.selectAll().executeAsList()
    assertTrue(players.any {
      it.last_name == "Karlsson"
    })
    closeDriver()
  }

  private fun getDb() = Db.instance
   */
}
