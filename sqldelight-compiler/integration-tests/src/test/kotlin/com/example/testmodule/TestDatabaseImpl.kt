package com.example.testmodule

import com.example.GroupQueries
import com.example.Player
import com.example.PlayerQueries
import com.example.SelectNull
import com.example.Team
import com.example.TeamForCoach
import com.example.TeamQueries
import com.example.TestDatabase
import com.example.team.SelectStuff
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.TransacterImpl
import com.squareup.sqldelight.`internal`.copyOnWriteList
import com.squareup.sqldelight.core.integration.Shoots
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import java.lang.Void
import kotlin.Any
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Unit
import kotlin.collections.Collection
import kotlin.collections.MutableList
import kotlin.jvm.JvmField
import kotlin.reflect.KClass

internal val KClass<TestDatabase>.schema: SqlDriver.Schema
  get() = TestDatabaseImpl.Schema

internal fun KClass<TestDatabase>.newInstance(
  driver: SqlDriver,
  playerAdapter: Player.Adapter,
  teamAdapter: Team.Adapter
): TestDatabase = TestDatabaseImpl(driver, playerAdapter, teamAdapter)

private class TestDatabaseImpl(
  driver: SqlDriver,
  internal val playerAdapter: Player.Adapter,
  internal val teamAdapter: Team.Adapter
) : TransacterImpl(driver), TestDatabase {
  public override val groupQueries: GroupQueriesImpl = GroupQueriesImpl(this, driver)

  public override val playerQueries: PlayerQueriesImpl = PlayerQueriesImpl(this, driver)

  public override val teamQueries: TeamQueriesImpl = TeamQueriesImpl(this, driver)

  public object Schema : SqlDriver.Schema {
    public override val version: Int
      get() = 1

    public override fun create(driver: SqlDriver): Unit {
      driver.execute(null, """
          |CREATE TABLE team (
          |  name TEXT PRIMARY KEY NOT NULL,
          |  captain INTEGER UNIQUE NOT NULL REFERENCES player(number),
          |  inner_type TEXT,
          |  coach TEXT NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |INSERT INTO team
          |VALUES ('Anaheim Ducks', 15, NULL, 'Randy Carlyle'),
          |       ('Ottawa Senators', 65, 'ONE', 'Guy Boucher')
          """.trimMargin(), 0)
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
          |INSERT INTO player
          |VALUES ('Ryan Getzlaf', 15, 'Anaheim Ducks', 'RIGHT'),
          |       ('Erik Karlsson', 65, 'Ottawa Senators', 'RIGHT')
          """.trimMargin(), 0)
      driver.execute(null, "CREATE TABLE `group` (`index` INTEGER PRIMARY KEY NOT NULL)", 0)
      driver.execute(null, "INSERT INTO `group` VALUES (1), (2), (3)", 0)
    }

    public override fun migrate(
      driver: SqlDriver,
      oldVersion: Int,
      newVersion: Int
    ): Unit {
    }
  }
}

private class TeamQueriesImpl(
  private val database: TestDatabaseImpl,
  private val driver: SqlDriver
) : TransacterImpl(driver), TeamQueries {
  internal val teamForCoach: MutableList<Query.Listener> = copyOnWriteList()

  internal val forInnerType: MutableList<Query.Listener> = copyOnWriteList()

  internal val selectStuff: MutableList<Query<*>> = copyOnWriteList()

  public override fun <T : Any> teamForCoach(coach: String, mapper: (name: String, captain: Long) ->
      T): Query<T> = TeamForCoachQuery(coach) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getLong(1)!!
    )
  }

  public override fun teamForCoach(coach: String): Query<TeamForCoach> = teamForCoach(coach) { name,
      captain ->
    TeamForCoach(
      name,
      captain
    )
  }

  public override fun <T : Any> forInnerType(inner_type: Shoots.Type?, mapper: (
    name: String,
    captain: Long,
    inner_type: Shoots.Type?,
    coach: String
  ) -> T): Query<T> = ForInnerTypeQuery(inner_type) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getLong(1)!!,
      cursor.getString(2)?.let { database.teamAdapter.inner_typeAdapter.decode(it) },
      cursor.getString(3)!!
    )
  }

  public override fun forInnerType(inner_type: Shoots.Type?): Query<Team> =
      forInnerType(inner_type) { name, captain, inner_type_, coach ->
    Team(
      name,
      captain,
      inner_type_,
      coach
    )
  }

  public override fun <T : Any> selectStuff(mapper: (expr: Long, expr_: Long) -> T): Query<T> =
      Query(397134288, selectStuff, driver, "Team.sq", "selectStuff", "SELECT 1, 2") { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1)!!
    )
  }

  public override fun selectStuff(): Query<SelectStuff> = selectStuff { expr, expr_ ->
    SelectStuff(
      expr,
      expr_
    )
  }

  private inner class TeamForCoachQuery<out T : Any>(
    @JvmField
    public val coach: String,
    mapper: (SqlCursor) -> T
  ) : Query<T>(teamForCoach, mapper) {
    public override fun execute(): SqlCursor = driver.executeQuery(1839882838, """
    |SELECT name, captain
    |FROM team
    |WHERE coach = ?
    """.trimMargin(), 1) {
      bindString(1, coach)
    }

    public override fun toString(): String = "Team.sq:teamForCoach"
  }

  private inner class ForInnerTypeQuery<out T : Any>(
    @JvmField
    public val inner_type: Shoots.Type?,
    mapper: (SqlCursor) -> T
  ) : Query<T>(forInnerType, mapper) {
    public override fun execute(): SqlCursor = driver.executeQuery(null, """
    |SELECT *
    |FROM team
    |WHERE inner_type ${ if (inner_type == null) "IS" else "=" } ?
    """.trimMargin(), 1) {
      bindString(1, inner_type?.let { database.teamAdapter.inner_typeAdapter.encode(it) })
    }

    public override fun toString(): String = "Team.sq:forInnerType"
  }
}

private class PlayerQueriesImpl(
  private val database: TestDatabaseImpl,
  private val driver: SqlDriver
) : TransacterImpl(driver), PlayerQueries {
  internal val allPlayers: MutableList<Query.Listener> = copyOnWriteList()

  internal val playersForTeam: MutableList<Query.Listener> = copyOnWriteList()

  internal val playersForNumbers: MutableList<Query.Listener> = copyOnWriteList()

  internal val selectNull: MutableList<Query.Listener> = copyOnWriteList()

  internal val selectStuff: MutableList<Query<*>> = copyOnWriteList()

  public override fun <T : Any> allPlayers(mapper: (
    name: String,
    number: Long,
    team: String?,
    shoots: Shoots
  ) -> T): Query<T> = Query(-1634440035, allPlayers, driver, "Player.sq", "allPlayers", """
  |SELECT *
  |FROM player
  """.trimMargin()) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getLong(1)!!,
      cursor.getString(2),
      database.playerAdapter.shootsAdapter.decode(cursor.getString(3)!!)
    )
  }

  public override fun allPlayers(): Query<Player> = allPlayers { name, number, team, shoots ->
    Player(
      name,
      number,
      team,
      shoots
    )
  }

  public override fun <T : Any> playersForTeam(team: String?, mapper: (
    name: String,
    number: Long,
    team: String?,
    shoots: Shoots
  ) -> T): Query<T> = PlayersForTeamQuery(team) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getLong(1)!!,
      cursor.getString(2),
      database.playerAdapter.shootsAdapter.decode(cursor.getString(3)!!)
    )
  }

  public override fun playersForTeam(team: String?): Query<Player> = playersForTeam(team) { name,
      number, team_, shoots ->
    Player(
      name,
      number,
      team_,
      shoots
    )
  }

  public override fun <T : Any> playersForNumbers(number: Collection<Long>, mapper: (
    name: String,
    number: Long,
    team: String?,
    shoots: Shoots
  ) -> T): Query<T> = PlayersForNumbersQuery(number) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getLong(1)!!,
      cursor.getString(2),
      database.playerAdapter.shootsAdapter.decode(cursor.getString(3)!!)
    )
  }

  public override fun playersForNumbers(number: Collection<Long>): Query<Player> =
      playersForNumbers(number) { name, number_, team, shoots ->
    Player(
      name,
      number_,
      team,
      shoots
    )
  }

  public override fun <T : Any> selectNull(mapper: (expr: Void?) -> T): Query<T> = Query(106890351,
      selectNull, driver, "Player.sq", "selectNull", "SELECT NULL") { cursor ->
    mapper(
      null
    )
  }

  public override fun selectNull(): Query<SelectNull> = selectNull { expr ->
    SelectNull(
      expr
    )
  }

  public override fun <T : Any> selectStuff(mapper: (expr: Long, expr_: Long) -> T): Query<T> =
      Query(-976770036, selectStuff, driver, "Player.sq", "selectStuff", "SELECT 1, 2") { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1)!!
    )
  }

  public override fun selectStuff(): Query<com.example.player.SelectStuff> = selectStuff { expr,
      expr_ ->
    com.example.player.SelectStuff(
      expr,
      expr_
    )
  }

  public override fun insertPlayer(
    name: String,
    number: Long,
    team: String?,
    shoots: Shoots
  ): Unit {
    driver.execute(-1595716666, """
    |INSERT INTO player
    |VALUES (?, ?, ?, ?)
    """.trimMargin(), 4) {
      bindString(1, name)
      bindLong(2, number)
      bindString(3, team)
      bindString(4, database.playerAdapter.shootsAdapter.encode(shoots))
    }
    notifyQueries { emit ->
      emit(database.playerQueries.allPlayers)
      emit(database.playerQueries.playersForTeam)
      emit(database.playerQueries.playersForNumbers)
    }
  }

  public override fun updateTeamForNumbers(team: String?, number: Collection<Long>): Unit {
    val numberIndexes = createArguments(count = number.size)
    driver.execute(null, """
    |UPDATE player
    |SET team = ?
    |WHERE number IN $numberIndexes
    """.trimMargin(), 1 + number.size) {
      bindString(1, team)
      number.forEachIndexed { index, number_ ->
          bindLong(index + 2, number_)
          }
    }
    notifyQueries { emit ->
      emit(database.playerQueries.allPlayers)
      emit(database.playerQueries.playersForTeam)
      emit(database.playerQueries.playersForNumbers)
    }
  }

  public override fun foreignKeysOn(): Unit {
    driver.execute(-1596558949, """PRAGMA foreign_keys = 1""", 0)
  }

  public override fun foreignKeysOff(): Unit {
    driver.execute(2046279987, """PRAGMA foreign_keys = 0""", 0)
  }

  private inner class PlayersForTeamQuery<out T : Any>(
    @JvmField
    public val team: String?,
    mapper: (SqlCursor) -> T
  ) : Query<T>(playersForTeam, mapper) {
    public override fun execute(): SqlCursor = driver.executeQuery(null, """
    |SELECT *
    |FROM player
    |WHERE team ${ if (team == null) "IS" else "=" } ?
    """.trimMargin(), 1) {
      bindString(1, team)
    }

    public override fun toString(): String = "Player.sq:playersForTeam"
  }

  private inner class PlayersForNumbersQuery<out T : Any>(
    @JvmField
    public val number: Collection<Long>,
    mapper: (SqlCursor) -> T
  ) : Query<T>(playersForNumbers, mapper) {
    public override fun execute(): SqlCursor {
      val numberIndexes = createArguments(count = number.size)
      return driver.executeQuery(null, """
      |SELECT *
      |FROM player
      |WHERE number IN $numberIndexes
      """.trimMargin(), number.size) {
        number.forEachIndexed { index, number_ ->
            bindLong(index + 1, number_)
            }
      }
    }

    public override fun toString(): String = "Player.sq:playersForNumbers"
  }
}

private class GroupQueriesImpl(
  private val database: TestDatabaseImpl,
  private val driver: SqlDriver
) : TransacterImpl(driver), GroupQueries {
  internal val selectAll: MutableList<Query.Listener> = copyOnWriteList()

  public override fun selectAll(): Query<Long> = Query(165688501, selectAll, driver, "Group.sq",
      "selectAll", "SELECT `index` FROM `group`") { cursor ->
    cursor.getLong(0)!!
  }
}
