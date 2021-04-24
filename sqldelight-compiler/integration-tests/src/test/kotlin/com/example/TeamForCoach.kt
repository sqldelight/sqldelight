package com.example

import kotlin.Long
import kotlin.String

public data class TeamForCoach(
  public val name: String,
  public val captain: Long
) {
  public override fun toString(): String = """
  |TeamForCoach [
  |  name: $name
  |  captain: $captain
  |]
  """.trimMargin()
}
