package com.example

import Query
import kotlin.Long

public class SomeTableQueries() {

  public fun selectAll(): Query<Example> {
    return Query(Example(1, "foo"))
  }

  public fun selectById(id: Long): Query<Example> {
    return Query(Example(1, "foo"))
  }
}
