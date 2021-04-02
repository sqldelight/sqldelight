package com.example

import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.core.integration.Shoots
import kotlin.Long
import kotlin.String

data class Player(
  val name: String,
  val number: Long,
  val team: String?,
  val shoots: Shoots
) {
  override fun toString(): String = """
  |Player [
  |  name: $name
  |  number: $number
  |  team: $team
  |  shoots: $shoots
  |]
  """.trimMargin()

  class Adapter(
    val shootsAdapter: ColumnAdapter<Shoots, String>
  )
}
