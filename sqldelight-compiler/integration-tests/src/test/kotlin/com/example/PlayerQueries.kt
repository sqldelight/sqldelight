package com.example

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.core.integration.Shoots
import java.lang.Void
import kotlin.Any
import kotlin.Long
import kotlin.String
import kotlin.Unit
import kotlin.collections.Collection

public interface PlayerQueries : Transacter {
  public fun <T : Any> allPlayers(mapper: (
    name: String,
    number: Long,
    team: String?,
    shoots: Shoots
  ) -> T): Query<T>

  public fun allPlayers(): Query<Player>

  public fun <T : Any> playersForTeam(team: String?, mapper: (
    name: String,
    number: Long,
    team: String?,
    shoots: Shoots
  ) -> T): Query<T>

  public fun playersForTeam(team: String?): Query<Player>

  public fun <T : Any> playersForNumbers(number: Collection<Long>, mapper: (
    name: String,
    number: Long,
    team: String?,
    shoots: Shoots
  ) -> T): Query<T>

  public fun playersForNumbers(number: Collection<Long>): Query<Player>

  public fun <T : Any> selectNull(mapper: (expr: Void?) -> T): Query<T>

  public fun selectNull(): Query<SelectNull>

  public fun insertPlayer(
    name: String,
    number: Long,
    team: String?,
    shoots: Shoots
  ): Unit

  public fun updateTeamForNumbers(team: String?, number: Collection<Long>): Unit

  public fun foreignKeysOn(): Unit

  public fun foreignKeysOff(): Unit
}
