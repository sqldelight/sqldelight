package com.example

import kotlin.Long
import kotlin.String

data class Group(
  val index: Long
) {
  override fun toString(): String = """
  |Group [
  |  index: $index
  |]
  """.trimMargin()
}
