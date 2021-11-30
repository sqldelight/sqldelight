package com.example

import app.cash.sqldelight.ColumnAdapter
import com.squareup.sqldelight.core.integration.Shoots
import kotlin.Long
import kotlin.String

public data class Team(
  public val name: String,
  public val captain: Long,
  public val inner_type: Shoots.Type?,
  public val coach: String
) {
  public override fun toString(): String = """
  |Team [
  |  name: $name
  |  captain: $captain
  |  inner_type: $inner_type
  |  coach: $coach
  |]
  """.trimMargin()

  public class Adapter(
    public val inner_typeAdapter: ColumnAdapter<Shoots.Type, String>
  )
}
