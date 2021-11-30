package com.example

import app.cash.sqldelight.ColumnAdapter
import kotlin.Int
import kotlin.String
import kotlin.collections.List

public data class New_test(
  public val first: String,
  public val second: List<Int>?
) {
  public override fun toString(): String = """
  |New_test [
  |  first: $first
  |  second: $second
  |]
  """.trimMargin()

  public class Adapter(
    public val secondAdapter: ColumnAdapter<List<Int>, String>
  )
}
