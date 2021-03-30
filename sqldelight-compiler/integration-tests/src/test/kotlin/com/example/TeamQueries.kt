package com.example

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.core.integration.Shoots
import kotlin.Any
import kotlin.Long
import kotlin.String

public interface TeamQueries : Transacter {
  public fun <T : Any> teamForCoach(coach: String, mapper: (name: String, captain: Long) -> T):
      Query<T>

  public fun teamForCoach(coach: String): Query<TeamForCoach>

  public fun <T : Any> forInnerType(inner_type: Shoots.Type?, mapper: (
    name: String,
    captain: Long,
    inner_type: Shoots.Type?,
    coach: String
  ) -> T): Query<T>

  public fun forInnerType(inner_type: Shoots.Type?): Query<Team>
}
