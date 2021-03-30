package com.example

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import kotlin.Long

public interface GroupQueries : Transacter {
  public fun selectAll(): Query<Long>
}
