package com.example

import Instant
import app.cash.sqldelight.ColumnAdapter
import java.time.OffsetDateTime

public data class Test(
  public val lastModifiedAt: Instant,
) {
  public class Adapter(
    public val lastModifiedAtAdapter: ColumnAdapter<Instant, OffsetDateTime>,
  )
}
