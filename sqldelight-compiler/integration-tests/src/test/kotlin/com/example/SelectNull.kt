package com.example

import java.lang.Void
import kotlin.String

interface SelectNull {
  val expr: Void?

  data class Impl(
    override val expr: Void?
  ) : SelectNull {
    override fun toString(): String = """
    |SelectNull.Impl [
    |  expr: $expr
    |]
    """.trimMargin()
  }
}
