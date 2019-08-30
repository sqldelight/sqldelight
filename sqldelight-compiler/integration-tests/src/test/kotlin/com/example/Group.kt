package com.example

import kotlin.Long
import kotlin.String

interface Group {
  val index: Long

  data class Impl(
    override val index: Long
  ) : Group {
    override fun toString(): String = """
    |Group.Impl [
    |  index: $index
    |]
    """.trimMargin()
  }
}
