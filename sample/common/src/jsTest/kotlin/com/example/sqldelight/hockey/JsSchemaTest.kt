package com.example.sqldelight.hockey

import com.example.sqldelight.hockey.data.Db
import com.example.sqldelight.hockey.data.Schema
import com.squareup.sqldelight.drivers.sqljs.initSqlDriver
import kotlin.js.Promise
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class JsSchemaTest {

  private lateinit var dbPromise: Promise<Unit>

  @BeforeTest
  fun setup() {
      dbPromise = initSqlDriver(Schema).then { Db.dbSetup(it) }
  }

  @AfterTest
  fun tearDown() {
      dbPromise.then { Db.dbClear() }
  }

  @Test
  fun teamsCreated() = dbPromise.then {
    val teams = getDb().teamQueries.selectAll().executeAsList()
    assertTrue(teams.any {
      it.name == "Anaheim Ducks"
    })
  }

  @Test
  fun playersCreated() = dbPromise.then {
    val players = getDb().playerQueries.selectAll().executeAsList()
    assertTrue(players.any {
      it.last_name == "Karlsson"
    })
  }

  private fun getDb() = Db.instance
}
