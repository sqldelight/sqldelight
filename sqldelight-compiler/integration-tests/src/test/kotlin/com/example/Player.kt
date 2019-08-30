package com.example

import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.core.integration.Shoots
import kotlin.Long
import kotlin.String

interface Player {
  val name: String

  val number: Long

  val team: String?

  val shoots: Shoots

  class Adapter(
    val shootsAdapter: ColumnAdapter<Shoots, String>
  )

  data class Impl(
    override val name: String,
    override val number: Long,
    override val team: String?,
    override val shoots: Shoots
  ) : Player {
    override fun toString(): String = """
    |Player.Impl [
    |  name: $name
    |  number: $number
    |  team: $team
    |  shoots: $shoots
    |]
    """.trimMargin()
  }
}
