package com.example

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import kotlin.Any
import kotlin.Int
import kotlin.String
import kotlin.collections.List

interface DataQueries : Transacter {
  fun <T : Any> migrationSelect(mapper: (first: String, second: List<Int>?) -> T): Query<T>

  fun migrationSelect(): Query<New_test>
}
