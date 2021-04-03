package com.example

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.core.integration.Shoots
import java.lang.Void
import kotlin.Any
import kotlin.Long
import kotlin.String
import kotlin.collections.Collection

interface PlayerQueries : Transacter {
  fun <T : Any> allPlayers(mapper: (
    name: String,
    number: Long,
    team: String?,
    shoots: Shoots
  ) -> T): Query<T>

  fun allPlayers(): Query<Player>

  fun <T : Any> playersForTeam(team: String?, mapper: (
    name: String,
    number: Long,
    team: String?,
    shoots: Shoots
  ) -> T): Query<T>

  fun playersForTeam(team: String?): Query<Player>

  fun <T : Any> playersForNumbers(number: Collection<Long>, mapper: (
    name: String,
    number: Long,
    team: String?,
    shoots: Shoots
  ) -> T): Query<T>

  fun playersForNumbers(number: Collection<Long>): Query<Player>

  fun <T : Any> selectNull(mapper: (expr: Void?) -> T): Query<T>

  fun selectNull(): Query<SelectNull>

  fun insertPlayer(
    name: String,
    number: Long,
    team: String?,
    shoots: Shoots
  )

  fun updateTeamForNumbers(team: String?, number: Collection<Long>)

  fun foreignKeysOn()

  fun foreignKeysOff()
}
