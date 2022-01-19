package com.example

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.core.integration.Shoots
import kotlin.Long
import kotlin.String

public data class Player(
  public val name: String,
  public val number: Long,
  public val team: String?,
  public val shoots: Shoots
) {
  public class Adapter(
    public val shootsAdapter: ColumnAdapter<Shoots, String>
  )
}
