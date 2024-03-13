package com.example.sqldelight.hockey.db

import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.adapter.primitive.FloatColumnAdapter
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.SqlDriver
import com.example.sqldelight.hockey.HockeyDb
import com.example.sqldelight.hockey.db.PlayerVals.Position
import com.example.sqldelight.hockey.db.PlayerVals.Shoots
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

expect fun webWorkerDriver(): SqlDriver

class DbHelper {
  private val driver: SqlDriver = webWorkerDriver()
  private var db: HockeyDb? = null

  private val mutex = Mutex()

  suspend fun withDatabase(block: suspend (HockeyDb) -> Unit): Unit = mutex.withLock {
    if (db == null) {
      db = createDb(driver)
      seedData(db!!)
    }

    block(db!!)
  }

  internal fun dbClear() {
    driver.close()
  }

  private suspend fun createDb(driver: SqlDriver): HockeyDb {
    HockeyDb.Schema.awaitCreate(driver)

    return HockeyDb(
      driver = driver,
      teamAdapter = Team.Adapter(
        foundedAdapter = DateAdapter(),
      ),
      playerAdapter = Player.Adapter(
        shootsAdapter = EnumColumnAdapter(),
        positionAdapter = EnumColumnAdapter(),
        birth_dateAdapter = DateAdapter(),
        numberAdapter = IntColumnAdapter,
        ageAdapter = IntColumnAdapter,
        weightAdapter = FloatColumnAdapter,
      ),
    )
  }

  private suspend fun seedData(db: HockeyDb) = db.apply {
    // Seed data time!
    val ducks = "Anaheim Ducks"
    val pens = "Pittsburgh Penguins"
    val sharks = "San Jose Sharks"
    val sens = "Ottawa Senators"

    // Populate teams.
    teamQueries.insertTeam(ducks, Date(1993, 3, 1), "Randy Carlyle", true)
    teamQueries.insertTeam(pens, Date(1966, 2, 8), "Mike Sullivan", true)
    teamQueries.insertTeam(sharks, Date(1990, 5, 5), "Peter DeBoer", false)
    teamQueries.insertTeam(sens, Date(1992, 10, 8), "D. J. Smith", false)

    playerQueries.insertPlayer(
      "Corey", "Perry", 10, ducks, 30, 210F, Date(1985, 5, 16),
      Shoots.RIGHT, Position.RIGHT_WING,
    )
    playerQueries.insertPlayer(
      "Ryan", "Getzlaf", 15, ducks, 30, 221F, Date(1985, 5, 10),
      Shoots.RIGHT, Position.CENTER,
    )
    teamQueries.setCaptain(15, ducks)

    playerQueries.insertPlayer(
      "Sidney", "Crosby", 87, pens, 28, 200F, Date(1987, 8, 7),
      Shoots.LEFT, Position.CENTER,
    )
    teamQueries.setCaptain(87, pens)

    playerQueries.insertPlayer(
      "Erik", "Karlsson", 65, sharks, 28, 190F, Date(1990, 5, 31),
      Shoots.RIGHT, Position.DEFENSE,
    )

    playerQueries.insertPlayer(
      "Joe", "Pavelski", 8, sharks, 31, 194F, Date(1984, 7, 18),
      Shoots.RIGHT, Position.CENTER,
    )
    teamQueries.setCaptain(8, sharks)

    playerQueries.insertPlayer(
      "Brady", "Tkachuk", 7, sens, 24, 221F, Date(1999, 9, 16), Shoots.LEFT, Position.LEFT_WING,
    )
    teamQueries.setCaptain(7, sens)
  }
}
