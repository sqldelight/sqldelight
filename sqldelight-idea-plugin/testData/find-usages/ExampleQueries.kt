package com.example

import Query
import kotlin.Long

public class ExampleQueries() {

  public fun selectById(id: Long): Query<Example> {
    return Query(Example(1, "foo"))
  }
}
