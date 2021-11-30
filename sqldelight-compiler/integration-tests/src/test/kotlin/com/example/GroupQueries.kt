package com.example

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import kotlin.Long

public interface GroupQueries : Transacter {
  public fun selectAll(): Query<Long>
}
