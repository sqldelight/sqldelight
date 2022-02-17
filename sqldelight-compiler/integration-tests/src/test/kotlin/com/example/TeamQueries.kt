package com.example

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.core.integration.Shoots
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import com.example.team.SelectStuff
import kotlin.Any
import kotlin.Long
import kotlin.String
import kotlin.Unit

public class TeamQueries(
  private val driver: SqlDriver<SqlPreparedStatement, SqlCursor>,
  private val teamAdapter: Team.Adapter
) : TransacterImpl(driver) {
  public fun <T : Any> teamForCoach(coach: String, mapper: (name: String, captain: Long) -> T):
      Query<T, SqlCursor> = TeamForCoachQuery(coach) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getLong(1)!!
    )
  }

  public fun teamForCoach(coach: String): Query<TeamForCoach, SqlCursor> = teamForCoach(coach) { name,
      captain ->
    TeamForCoach(
      name,
      captain
    )
  }

  public fun <T : Any> forInnerType(inner_type: Shoots.Type?, mapper: (
    name: String,
    captain: Long,
    inner_type: Shoots.Type?,
    coach: String
  ) -> T): Query<T, SqlCursor> = ForInnerTypeQuery(inner_type) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getLong(1)!!,
      cursor.getString(2)?.let { teamAdapter.inner_typeAdapter.decode(it) },
      cursor.getString(3)!!
    )
  }

  public fun forInnerType(inner_type: Shoots.Type?): Query<Team, SqlCursor> = forInnerType(inner_type) { name,
      captain, inner_type_, coach ->
    Team(
      name,
      captain,
      inner_type_,
      coach
    )
  }

  public fun <T : Any> selectStuff(mapper: (expr: Long, expr_: Long) -> T): Query<T, SqlCursor> =
      Query(397134288, emptyArray(), driver, "Team.sq", "selectStuff", "SELECT 1, 2") { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1)!!
    )
  }

  public fun selectStuff(): Query<SelectStuff, SqlCursor> = selectStuff { expr, expr_ ->
    SelectStuff(
      expr,
      expr_
    )
  }

  private inner class TeamForCoachQuery<out T : Any>(
    public val coach: String,
    mapper: (SqlCursor) -> T
  ) : Query<T, SqlCursor>(mapper) {
    public override fun addListener(listener: Query.Listener): Unit {
      driver.addListener(listener, arrayOf("team"))
    }

    public override fun removeListener(listener: Query.Listener): Unit {
      driver.removeListener(listener, arrayOf("team"))
    }

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
    public val inner_type: Shoots.Type?,
    mapper: (SqlCursor) -> T
  ) : Query<T, SqlCursor>(mapper) {
    public override fun addListener(listener: Query.Listener): Unit {
      driver.addListener(listener, arrayOf("team"))
    }

    public override fun removeListener(listener: Query.Listener): Unit {
      driver.removeListener(listener, arrayOf("team"))
    }

    public override fun execute(): SqlCursor = driver.executeQuery(null, """
    |SELECT *
    |FROM team
    |WHERE inner_type ${ if (inner_type == null) "IS" else "=" } ?
    """.trimMargin(), 1) {
      bindString(1, inner_type?.let { teamAdapter.inner_typeAdapter.encode(it) })
    }

    public override fun toString(): String = "Team.sq:forInnerType"
  }
}
