package com.example

import app.cash.sqldelight.ColumnAdapter
import kotlin.Int
import kotlin.String
import kotlin.collections.List

public data class Test(
  public val third: String,
  public val second: List<Int>?,
) {
  public class Adapter(
    public val secondAdapter: ColumnAdapter<List<Int>, String>,
  )
}
