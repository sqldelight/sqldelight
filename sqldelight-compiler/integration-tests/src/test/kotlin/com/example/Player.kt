package com.example

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.core.integration.Shoots
import kotlin.Long
import kotlin.String
import kotlin.jvm.JvmInline

public data class Player(
  public val name: Name,
  public val number: Long,
  public val team: Team.Name?,
  public val shoots: Shoots,
) {
  public class Adapter(
    public val shootsAdapter: ColumnAdapter<Shoots, String>,
  )

  @JvmInline
  public value class Name(
    public val name: String,
  )
}
