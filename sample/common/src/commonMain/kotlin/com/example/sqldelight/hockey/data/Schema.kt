package com.example.sqldelight.hockey.data

import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.example.sqldelight.hockey.HockeyDb
import com.example.sqldelight.hockey.data.PlayerVals.Position
import com.example.sqldelight.hockey.data.PlayerVals.Shoots

fun createQueryWrapper(driver: SqlDriver): HockeyDb {
  return HockeyDb(
    driver = driver,
    teamAdapter = Team.Adapter(
      foundedAdapter = DateAdapter()
    ),
    playerAdapter = Player.Adapter(
      shootsAdapter = EnumColumnAdapter(),
      positionAdapter = EnumColumnAdapter(),
      birth_dateAdapter = DateAdapter()
    )
  )
}

object Schema : SqlDriver.Schema by HockeyDb.Schema {
  override fun create(driver: SqlDriver) {
    HockeyDb.Schema.create(driver)

    // Seed data time!
    createQueryWrapper(driver).apply {

      val ducks = "Anaheim Ducks"
      val pens = "Pittsburgh Penguins"
      val sharks = "San Jose Sharks"

      // Populate teams.
      teamQueries.insertTeam(ducks, Date(1993, 3, 1), "Randy Carlyle", true)
      teamQueries.insertTeam(pens, Date(1966, 2, 8), "Mike Sullivan", true)
      teamQueries.insertTeam(sharks, Date(1990, 5, 5), "Peter DeBoer", false)

      playerQueries.insertPlayer(
        "Corey", "Perry", 10, ducks, 30, 210F, Date(1985, 5, 16),
        Shoots.RIGHT, Position.RIGHT_WING
      )
      playerQueries.insertPlayer(
        "Ryan", "Getzlaf", 15, ducks, 30, 221F, Date(1985, 5, 10),
        Shoots.RIGHT, Position.CENTER
      )
      teamQueries.setCaptain(15, ducks)

      playerQueries.insertPlayer(
        "Sidney", "Crosby", 87, pens, 28, 200F, Date(1987, 8, 7),
        Shoots.LEFT, Position.CENTER
      )
      teamQueries.setCaptain(87, pens)

      playerQueries.insertPlayer(
        "Erik", "Karlsson", 65, sharks, 28, 190F, Date(1990, 5, 31),
        Shoots.RIGHT, Position.DEFENSE
      )

      playerQueries.insertPlayer(
        "Joe", "Pavelski", 8, sharks, 31, 194F, Date(1984, 7, 18),
        Shoots.RIGHT, Position.CENTER
      )
      teamQueries.setCaptain(8, sharks)
    }
  }
}
