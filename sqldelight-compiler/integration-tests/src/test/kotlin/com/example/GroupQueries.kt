package com.example

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import kotlin.Long

interface GroupQueries : Transacter {
  fun selectAll(): Query<Long>
}
