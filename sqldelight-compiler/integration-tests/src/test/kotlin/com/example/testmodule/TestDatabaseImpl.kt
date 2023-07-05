package com.example.testmodule

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.example.GroupQueries
import com.example.Player
import com.example.PlayerQueries
import com.example.Team
import com.example.TeamQueries
import com.example.TestDatabase
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass

internal val KClass<TestDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
  get() = TestDatabaseImpl.Schema

internal fun KClass<TestDatabase>.newInstance(
  driver: SqlDriver,
  playerAdapter: Player.Adapter,
  teamAdapter: Team.Adapter,
): TestDatabase = TestDatabaseImpl(driver, playerAdapter, teamAdapter)

private class TestDatabaseImpl(
  driver: SqlDriver,
  playerAdapter: Player.Adapter,
  teamAdapter: Team.Adapter,
) : TransacterImpl(driver), TestDatabase {
  override val groupQueries: GroupQueries = GroupQueries(driver)

  override val playerQueries: PlayerQueries = PlayerQueries(driver, playerAdapter)

  override val teamQueries: TeamQueries = TeamQueries(driver, teamAdapter)

  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long
      get() = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(null, "CREATE TABLE `group` (`index` INTEGER PRIMARY KEY NOT NULL)", 0)
      driver.execute(null, """
          |CREATE TABLE player (
          |  name TEXT NOT NULL,
          |  number INTEGER NOT NULL,
          |  team TEXT REFERENCES team(name),
          |  shoots TEXT NOT NULL,
          |  PRIMARY KEY (team, number)
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE team (
          |  name TEXT PRIMARY KEY NOT NULL,
          |  captain INTEGER UNIQUE NOT NULL REFERENCES player(number),
          |  inner_type TEXT,
          |  coach TEXT NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, "INSERT INTO `group` VALUES (1), (2), (3)", 0)
      driver.execute(null, "CREATE VIRTUAL TABLE myftstable USING fts5(something, nice)", 0)
      driver.execute(null, "CREATE VIRTUAL TABLE myftstable2 USING fts5(something, nice)", 0)
      driver.execute(null, """
          |INSERT INTO player
          |VALUES ('Ryan Getzlaf', 15, 'Anaheim Ducks', 'RIGHT'),
          |       ('Erik Karlsson', 65, 'Ottawa Senators', 'RIGHT')
          """.trimMargin(), 0)
      driver.execute(null, """
          |INSERT INTO team
          |VALUES ('Anaheim Ducks', 15, NULL, 'Randy Carlyle'),
          |       ('Ottawa Senators', 65, 'ONE', 'Guy Boucher')
          """.trimMargin(), 0)
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> = QueryResult.Unit
  }
}
