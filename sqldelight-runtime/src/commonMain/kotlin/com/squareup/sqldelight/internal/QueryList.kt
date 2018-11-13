package com.squareup.sqldelight.internal

import co.touchlab.stately.collections.frozenCopyOnWriteList
import com.squareup.sqldelight.Query
import co.touchlab.stately.concurrency.*

/**
 * TODO: Get rid of if CopyOnWriteArrayList joins kotlin.collections?
 */
class QueryList {
  internal var queries: List<Query<*>> = frozenCopyOnWriteList()
  private val queryLock = QuickLock()
  fun addQuery(query: Query<*>) {
    queryLock.withLock {
      queries += query
    }
  }

  fun removeQuery(query: Query<*>) {
    queryLock.withLock {
      queries -= query
    }
  }

  operator fun plus(other: QueryList): QueryList {
    val result = QueryList()
    result.queries = queries + other.queries
    return result
  }
}

