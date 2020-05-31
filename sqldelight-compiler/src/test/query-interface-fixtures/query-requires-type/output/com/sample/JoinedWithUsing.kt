package com.sample

import kotlin.Boolean
import kotlin.String

data class JoinedWithUsing(
  val name: String,
  val is_cool: Boolean
) {
  override fun toString(): String = """
  |JoinedWithUsing [
  |  name: $name
  |  is_cool: $is_cool
  |]
  """.trimMargin()
}
