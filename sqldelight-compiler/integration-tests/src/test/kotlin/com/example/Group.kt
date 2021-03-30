package com.example

import kotlin.Long
import kotlin.String

public data class Group(
  public val index: Long
) {
  public override fun toString(): String = """
  |Group [
  |  index: $index
  |]
  """.trimMargin()
}
