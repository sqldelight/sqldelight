package com.sample

import kotlin.Boolean
import kotlin.String

public data class JoinedWithUsing(
  public val name: String,
  public val is_cool: Boolean
) {
  public override fun toString(): String = """
  |JoinedWithUsing [
  |  name: $name
  |  is_cool: $is_cool
  |]
  """.trimMargin()
}
