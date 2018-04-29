package com.squareup.sqldelight.internal

import com.squareup.sqldelight.Query

/**
 * TODO: Get rid of if CopyOnWriteArrayList joins kotlin.collections?
 */
class QueryList {
  internal var queries: List<Query<*>> = emptyList()

  fun addQuery(query: Query<*>) {
    synchronized(queries) {
      queries += query
    }
  }

  fun removeQuery(query: Query<*>) {
    synchronized(queries) {
      queries -= query
    }
  }

  operator fun plus(other: QueryList): QueryList {
    val result = QueryList()
    result.queries = queries + other.queries
    return result
  }
}

