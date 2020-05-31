package com.example

import java.lang.Void
import kotlin.String

data class SelectNull(
  val expr: Void?
) {
  override fun toString(): String = """
  |SelectNull [
  |  expr: $expr
  |]
  """.trimMargin()
}
