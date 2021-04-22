package com.example.team

import kotlin.Long
import kotlin.String

public data class SelectStuff(
  public val expr: Long,
  public val expr_: Long
) {
  public override fun toString(): String = """
  |SelectStuff [
  |  expr: $expr
  |  expr_: $expr_
  |]
  """.trimMargin()
}
