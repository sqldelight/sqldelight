package com.example

import kotlin.Long
import kotlin.String

data class TeamForCoach(
  val name: String,
  val captain: Long
) {
  override fun toString(): String = """
  |TeamForCoach [
  |  name: $name
  |  captain: $captain
  |]
  """.trimMargin()
}
