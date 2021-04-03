package com.example

import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.core.integration.Shoots
import kotlin.Long
import kotlin.String

data class Team(
  val name: String,
  val captain: Long,
  val inner_type: Shoots.Type?,
  val coach: String
) {
  override fun toString(): String = """
  |Team [
  |  name: $name
  |  captain: $captain
  |  inner_type: $inner_type
  |  coach: $coach
  |]
  """.trimMargin()

  class Adapter(
    val inner_typeAdapter: ColumnAdapter<Shoots.Type, String>
  )
}
