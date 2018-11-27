package com.squareup.sqldelight.internal

import co.touchlab.stately.collections.frozenCopyOnWriteList
import co.touchlab.stately.concurrency.QuickLock
import co.touchlab.stately.concurrency.withLock
import com.squareup.sqldelight.Query

/**
 * TODO: Get rid of if CopyOnWriteArrayList joins kotlin.collections?
 */
class QueryList {
  internal val queries: MutableList<Query<*>> = frozenCopyOnWriteList()
  private val queryLock = QuickLock()
  fun addQuery(query: Query<*>) {
    queryLock.withLock {
      queries.add(query)
    }
  }

  fun removeQuery(query: Query<*>) {
    queryLock.withLock {
      queries.remove(query)
    }
  }

  operator fun plus(other: QueryList): QueryList {
    val result = QueryList()
    result.queries.addAll(queries)
    result.queries.addAll(other.queries)
    return result
  }
}

