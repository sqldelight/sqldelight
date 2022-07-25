// Copyright Square, Inc.
package app.cash.sqldelight.paging3.util

import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("UNCHECKED_CAST")
class ThreadSafeInvalidationObserver(
  tables: Array<out String>,
  val onInvalidated: () -> Unit,
) : InvalidationTracker.Observer(tables = tables as Array<String>) {
  private val registered: AtomicBoolean = AtomicBoolean(false)

  override fun onInvalidated(tables: Set<String>) {
    onInvalidated()
  }

  fun registerIfNecessary(db: RoomDatabase) {
    if (registered.compareAndSet(false, true)) {
      db.invalidationTracker.addWeakObserver(this)
    }
  }
}
