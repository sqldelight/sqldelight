package com.squareup.sqldelight.drivers.native

import co.touchlab.sqliter.Cursor
import co.touchlab.sqliter.getBytesOrNull
import co.touchlab.sqliter.getDoubleOrNull
import co.touchlab.sqliter.getLongOrNull
import co.touchlab.sqliter.getStringOrNull
import co.touchlab.stately.concurrency.AtomicInt
import co.touchlab.stately.concurrency.ThreadRef
import com.squareup.sqldelight.db.SqlCursor
import platform.Foundation.NSThread

/**
 * Wrapper for cursor calls. Cursors point to real SQLite statements, so we need to be careful with
 * them. If dev closes the outer structure, this will get closed as well, which means it could start
 * throwing errors if you're trying to access it.
 */
internal class SqliterSqlCursor(
  private val cursor: Cursor,
//  private val detatch: () -> Unit,
  private val recycler: () -> Unit
) : SqlCursor {
  private val threadRef: ThreadRef = ThreadRef()
  private val detachCalled = AtomicInt(0)

  private inline fun <T> checkDetach(block:() -> T):T {
//    if(!threadRef.same() && detachCalled.compareAndSet(0, 1))
//      detatch()
    return block()
  }
  override fun close() = checkDetach {
    recycler()
  }

  override fun getBytes(index: Int): ByteArray? = checkDetach { cursor.getBytesOrNull(index) }

  override fun getDouble(index: Int): Double? = checkDetach { cursor.getDoubleOrNull(index) }

  override fun getLong(index: Int): Long? = checkDetach { cursor.getLongOrNull(index) }

  override fun getString(index: Int): String? = checkDetach { cursor.getStringOrNull(index) }

  override fun next(): Boolean = checkDetach { cursor.next() }
}
