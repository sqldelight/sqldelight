package com.example

import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.core.integration.Shoots
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import com.example.player.SelectStuff
import java.lang.Void
import kotlin.Any
import kotlin.Long
import kotlin.String
import kotlin.collections.Collection

public class PlayerQueries(
  driver: SqlDriver,
  private val playerAdapter: Player.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> insertAndReturn(
    name: Player.Name,
    number: Long,
    team: Team.Name?,
    shoots: Shoots,
    mapper: (
      name: Player.Name,
      number: Long,
      team: Team.Name?,
      shoots: Shoots,
    ) -> T,
  ): ExecutableQuery<T> = InsertAndReturnQuery(name, number, team, shoots) { cursor ->
    mapper(
      Player.Name(cursor.getString(0)!!),
      cursor.getLong(1)!!,
      cursor.getString(2)?.let { Team.Name(it) },
      playerAdapter.shootsAdapter.decode(cursor.getString(3)!!)
    )
  }

  public fun insertAndReturn(
    name: Player.Name,
    number: Long,
    team: Team.Name?,
    shoots: Shoots,
  ): ExecutableQuery<Player> = insertAndReturn(name, number, team, shoots) { name_, number_, team_,
      shoots_ ->
    Player(
      name_,
      number_,
      team_,
      shoots_
    )
  }

  public fun <T : Any> allPlayers(mapper: (
    name: Player.Name,
    number: Long,
    team: Team.Name?,
    shoots: Shoots,
  ) -> T): Query<T> = Query(-1_634_440_035, arrayOf("player"), driver, "Player.sq", "allPlayers",
      """
  |SELECT *
  |FROM player
  """.trimMargin()) { cursor ->
    mapper(
      Player.Name(cursor.getString(0)!!),
      cursor.getLong(1)!!,
      cursor.getString(2)?.let { Team.Name(it) },
      playerAdapter.shootsAdapter.decode(cursor.getString(3)!!)
    )
  }

  public fun allPlayers(): Query<Player> = allPlayers { name, number, team, shoots ->
    Player(
      name,
      number,
      team,
      shoots
    )
  }

  public fun <T : Any> playersForTeam(team: Team.Name?, mapper: (
    name: Player.Name,
    number: Long,
    team: Team.Name?,
    shoots: Shoots,
  ) -> T): Query<T> = PlayersForTeamQuery(team) { cursor ->
    mapper(
      Player.Name(cursor.getString(0)!!),
      cursor.getLong(1)!!,
      cursor.getString(2)?.let { Team.Name(it) },
      playerAdapter.shootsAdapter.decode(cursor.getString(3)!!)
    )
  }

  public fun playersForTeam(team: Team.Name?): Query<Player> = playersForTeam(team) { name, number,
      team_, shoots ->
    Player(
      name,
      number,
      team_,
      shoots
    )
  }

  public fun <T : Any> playersForNumbers(number: Collection<Long>, mapper: (
    name: Player.Name,
    number: Long,
    team: Team.Name?,
    shoots: Shoots,
  ) -> T): Query<T> = PlayersForNumbersQuery(number) { cursor ->
    mapper(
      Player.Name(cursor.getString(0)!!),
      cursor.getLong(1)!!,
      cursor.getString(2)?.let { Team.Name(it) },
      playerAdapter.shootsAdapter.decode(cursor.getString(3)!!)
    )
  }

  public fun playersForNumbers(number: Collection<Long>): Query<Player> =
      playersForNumbers(number) { name, number_, team, shoots ->
    Player(
      name,
      number_,
      team,
      shoots
    )
  }

  public fun <T : Any> selectNull(mapper: (expr: Void?) -> T): ExecutableQuery<T> =
      Query(106_890_351, driver, "Player.sq", "selectNull", "SELECT NULL") { cursor ->
    mapper(
      null
    )
  }

  public fun selectNull(): ExecutableQuery<SelectNull> = selectNull { expr ->
    SelectNull(
      expr
    )
  }

  public fun <T : Any> selectStuff(mapper: (expr: Long, expr_: Long) -> T): ExecutableQuery<T> =
      Query(-976_770_036, driver, "Player.sq", "selectStuff", "SELECT 1, 2") { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1)!!
    )
  }

  public fun selectStuff(): ExecutableQuery<SelectStuff> = selectStuff { expr, expr_ ->
    SelectStuff(
      expr,
      expr_
    )
  }

  public fun <T : Any> greaterThanNumberAndName(
    number: Long,
    name: Player.Name,
    mapper: (
      name: Player.Name,
      number: Long,
      team: Team.Name?,
      shoots: Shoots,
    ) -> T,
  ): Query<T> = GreaterThanNumberAndNameQuery(number, name) { cursor ->
    mapper(
      Player.Name(cursor.getString(0)!!),
      cursor.getLong(1)!!,
      cursor.getString(2)?.let { Team.Name(it) },
      playerAdapter.shootsAdapter.decode(cursor.getString(3)!!)
    )
  }

  public fun greaterThanNumberAndName(number: Long, name: Player.Name): Query<Player> =
      greaterThanNumberAndName(number, name) { name_, number_, team, shoots ->
    Player(
      name_,
      number_,
      team,
      shoots
    )
  }

  public fun insertPlayer(
    name: Player.Name,
    number: Long,
    team: Team.Name?,
    shoots: Shoots,
  ) {
    driver.execute(-1_595_716_666, """
        |INSERT INTO player
        |VALUES (?, ?, ?, ?)
        """.trimMargin(), 4) {
          bindString(0, name.name)
          bindLong(1, number)
          bindString(2, team?.let { it.name })
          bindString(3, playerAdapter.shootsAdapter.encode(shoots))
        }
    notifyQueries(-1_595_716_666) { emit ->
      emit("player")
    }
  }

  public fun updateTeamForNumbers(team: Team.Name?, number: Collection<Long>) {
    val numberIndexes = createArguments(count = number.size)
    driver.execute(null, """
        |UPDATE player
        |SET team = ?
        |WHERE number IN $numberIndexes
        """.trimMargin(), 1 + number.size) {
          bindString(0, team?.let { it.name })
          number.forEachIndexed { index, number_ ->
            bindLong(index + 1, number_)
          }
        }
    notifyQueries(-636_585_613) { emit ->
      emit("player")
    }
  }

  public fun foreignKeysOn() {
    driver.execute(-1_596_558_949, """PRAGMA foreign_keys = 1""", 0)
  }

  public fun foreignKeysOff() {
    driver.execute(2_046_279_987, """PRAGMA foreign_keys = 0""", 0)
  }

  private inner class InsertAndReturnQuery<out T : Any>(
    public val name: Player.Name,
    public val number: Long,
    public val team: Team.Name?,
    public val shoots: Shoots,
    mapper: (SqlCursor) -> T,
  ) : ExecutableQuery<T>(mapper) {
    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        transactionWithResult {
      driver.execute(-452_007_405, """
          |INSERT INTO player
          |  VALUES (?, ?, ?, ?)
          """.trimMargin(), 4) {
            bindString(0, name.name)
            bindLong(1, number)
            bindString(2, team?.let { it.name })
            bindString(3, playerAdapter.shootsAdapter.encode(shoots))
          }
      driver.executeQuery(-452_007_404, """
          |SELECT *
          |  FROM player
          |  WHERE player.rowid = last_insert_rowid()
          """.trimMargin(), mapper, 0)
    }

    override fun toString(): String = "Player.sq:insertAndReturn"
  }

  private inner class PlayersForTeamQuery<out T : Any>(
    public val team: Team.Name?,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("player", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("player", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null, """
    |SELECT *
    |FROM player
    |WHERE team ${ if (team == null) "IS" else "=" } ?
    """.trimMargin(), mapper, 1) {
      bindString(0, team?.let { it.name })
    }

    override fun toString(): String = "Player.sq:playersForTeam"
  }

  private inner class PlayersForNumbersQuery<out T : Any>(
    public val number: Collection<Long>,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("player", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("player", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> {
      val numberIndexes = createArguments(count = number.size)
      return driver.executeQuery(null, """
          |SELECT *
          |FROM player
          |WHERE number IN $numberIndexes
          """.trimMargin(), mapper, number.size) {
            number.forEachIndexed { index, number_ ->
              bindLong(index, number_)
            }
          }
    }

    override fun toString(): String = "Player.sq:playersForNumbers"
  }

  private inner class GreaterThanNumberAndNameQuery<out T : Any>(
    public val number: Long,
    public val name: Player.Name,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("player", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("player", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_258_650_806, """
    |SELECT *
    |FROM player
    |WHERE (number, name) > (?, ?)
    """.trimMargin(), mapper, 2) {
      bindLong(0, number)
      bindString(1, name.name)
    }

    override fun toString(): String = "Player.sq:greaterThanNumberAndName"
  }
}
