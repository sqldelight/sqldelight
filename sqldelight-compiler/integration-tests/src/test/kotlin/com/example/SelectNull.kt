package com.example

import java.lang.Void
import kotlin.String

public data class SelectNull(
  public val expr: Void?
) {
  public override fun toString(): String = """
  |SelectNull [
  |  expr: $expr
  |]
  """.trimMargin()
}
