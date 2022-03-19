package com.example

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.core.integration.Shoots
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import com.example.player.SelectStuff
import java.lang.Void
import kotlin.Any
import kotlin.Long
import kotlin.String
import kotlin.Unit
import kotlin.collections.Collection

public class PlayerQueries(
  private val driver: SqlDriver,
  private val playerAdapter: Player.Adapter
) : TransacterImpl(driver) {
  public fun <T : Any> allPlayers(mapper: (
    name: String,
    number: Long,
    team: String?,
    shoots: Shoots
  ) -> T): Query<T> = Query(-1634440035, arrayOf("player"), driver, "Player.sq", "allPlayers", """
  |SELECT *
  |FROM player
  """.trimMargin()) { cursor ->
    check(cursor is SqlCursor)
    mapper(
      cursor.getString(0)!!,
      cursor.getLong(1)!!,
      cursor.getString(2),
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

  public fun <T : Any> playersForTeam(team: String?, mapper: (
    name: String,
    number: Long,
    team: String?,
    shoots: Shoots
  ) -> T): Query<T> = PlayersForTeamQuery(team) { cursor ->
    check(cursor is SqlCursor)
    mapper(
      cursor.getString(0)!!,
      cursor.getLong(1)!!,
      cursor.getString(2),
      playerAdapter.shootsAdapter.decode(cursor.getString(3)!!)
    )
  }

  public fun playersForTeam(team: String?): Query<Player> = playersForTeam(team) { name, number,
      team_, shoots ->
    Player(
      name,
      number,
      team_,
      shoots
    )
  }

  public fun <T : Any> playersForNumbers(number: Collection<Long>, mapper: (
    name: String,
    number: Long,
    team: String?,
    shoots: Shoots
  ) -> T): Query<T> = PlayersForNumbersQuery(number) { cursor ->
    check(cursor is SqlCursor)
    mapper(
      cursor.getString(0)!!,
      cursor.getLong(1)!!,
      cursor.getString(2),
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

  public fun <T : Any> selectNull(mapper: (expr: Void?) -> T): Query<T> = Query(106890351,
      emptyArray(), driver, "Player.sq", "selectNull", "SELECT NULL") { cursor ->
    check(cursor is SqlCursor)
    mapper(
      null
    )
  }

  public fun selectNull(): Query<SelectNull> = selectNull { expr ->
    SelectNull(
      expr
    )
  }

  public fun <T : Any> selectStuff(mapper: (expr: Long, expr_: Long) -> T): Query<T> =
      Query(-976770036, emptyArray(), driver, "Player.sq", "selectStuff", "SELECT 1, 2") { cursor ->
    check(cursor is SqlCursor)
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1)!!
    )
  }

  public fun selectStuff(): Query<SelectStuff> = selectStuff { expr, expr_ ->
    SelectStuff(
      expr,
      expr_
    )
  }

  public fun insertPlayer(
    name: String,
    number: Long,
    team: String?,
    shoots: Shoots
  ): Unit {
    driver.execute(-1595716666, """
    |INSERT INTO player
    |VALUES (?, ?, ?, ?)
    """.trimMargin(), 4) {
      check(this is SqlPreparedStatement)
      bindString(1, name)
      bindLong(2, number)
      bindString(3, team)
      bindString(4, playerAdapter.shootsAdapter.encode(shoots))
    }
    notifyQueries(-1595716666) { emit ->
      emit("player")
    }
  }

  public fun updateTeamForNumbers(team: String?, number: Collection<Long>): Unit {
    val numberIndexes = createArguments(count = number.size)
    driver.execute(null, """
    |UPDATE player
    |SET team = ?
    |WHERE number IN $numberIndexes
    """.trimMargin(), 1 + number.size) {
      check(this is SqlPreparedStatement)
      bindString(1, team)
      number.forEachIndexed { index, number_ ->
          bindLong(index + 2, number_)
          }
    }
    notifyQueries(-636585613) { emit ->
      emit("player")
    }
  }

  public fun foreignKeysOn(): Unit {
    driver.execute(-1596558949, """PRAGMA foreign_keys = 1""", 0)
  }

  public fun foreignKeysOff(): Unit {
    driver.execute(2046279987, """PRAGMA foreign_keys = 0""", 0)
  }

  private inner class PlayersForTeamQuery<out T : Any>(
    public val team: String?,
    mapper: (SqlCursor) -> T
  ) : Query<T>(mapper) {
    public override fun addListener(listener: Query.Listener): Unit {
      driver.addListener(listener, arrayOf("player"))
    }

    public override fun removeListener(listener: Query.Listener): Unit {
      driver.removeListener(listener, arrayOf("player"))
    }

    public override fun execute(): SqlCursor = driver.executeQuery(null, """
    |SELECT *
    |FROM player
    |WHERE team ${ if (team == null) "IS" else "=" } ?
    """.trimMargin(), 1) {
      check(this is SqlPreparedStatement)
      bindString(1, team)
    }

    public override fun toString(): String = "Player.sq:playersForTeam"
  }

  private inner class PlayersForNumbersQuery<out T : Any>(
    public val number: Collection<Long>,
    mapper: (SqlCursor) -> T
  ) : Query<T>(mapper) {
    public override fun addListener(listener: Query.Listener): Unit {
      driver.addListener(listener, arrayOf("player"))
    }

    public override fun removeListener(listener: Query.Listener): Unit {
      driver.removeListener(listener, arrayOf("player"))
    }

    public override fun execute(): SqlCursor {
      val numberIndexes = createArguments(count = number.size)
      return driver.executeQuery(null, """
      |SELECT *
      |FROM player
      |WHERE number IN $numberIndexes
      """.trimMargin(), number.size) {
        check(this is SqlPreparedStatement)
        number.forEachIndexed { index, number_ ->
            bindLong(index + 1, number_)
            }
      }
    }

    public override fun toString(): String = "Player.sq:playersForNumbers"
  }
}
