package com.example

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import kotlin.Any
import kotlin.Int
import kotlin.String
import kotlin.collections.List

public interface DataQueries : Transacter {
  public fun <T : Any> migrationSelect(mapper: (first: String, second: List<Int>?) -> T): Query<T>

  public fun migrationSelect(): Query<New_test>
}
