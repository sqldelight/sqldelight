package com.example

import com.squareup.sqldelight.ColumnAdapter
import kotlin.Int
import kotlin.String
import kotlin.collections.List

data class New_test(
  val first: String,
  val second: List<Int>?
) {
  override fun toString(): String = """
  |New_test [
  |  first: $first
  |  second: $second
  |]
  """.trimMargin()

  class Adapter(
    val secondAdapter: ColumnAdapter<List<Int>, String>
  )
}
