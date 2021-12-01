package app.cash.sqldelight.driver.native

import app.cash.sqldelight.db.Closeable
import app.cash.sqldelight.driver.native.util.PoolLock
import co.touchlab.stately.concurrency.AtomicBoolean
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze

/**
 * A shared pool of connections. Borrowing is blocking when all connections are in use, and the pool has reached its
 * designated capacity.
 */
internal class Pool<T : Closeable>(internal val capacity: Int, private val producer: () -> T) {
  /**
   * Hold a list of active connections. If it is null, it means the MultiPool has been closed.
   */
  private val entriesRef = AtomicReference<List<Entry>?>(listOf<Entry>().freeze())
  private val poolLock = PoolLock()

  /**
   * For test purposes only
   */
  internal fun entryCount(): Int = poolLock.withLock {
    entriesRef.value?.size ?: 0
  }

  fun borrowEntry(): Borrowed<T> {
    val snapshot = entriesRef.value ?: throw ClosedMultiPoolException

    // Fastpath: Borrow the first available entry.
    val firstAvailable = snapshot.firstOrNull { it.tryToAcquire() }

    if (firstAvailable != null) {
      return firstAvailable.asBorrowed(poolLock)
    }

    // Slowpath: Create a new entry if capacity limit has not been reached, or wait for the next available entry.
    val nextAvailable = poolLock.withLock {
      // Reload the list since it could've been updated by other threads concurrently.
      val entries = entriesRef.value ?: throw ClosedMultiPoolException

      if (entries.count() < capacity) {
        // Capacity hasn't been reached — create a new entry to serve this call.
        val newEntry = Entry(producer())
        val done = newEntry.tryToAcquire()
        check(done)

        entriesRef.value = (entries + listOf(newEntry)).freeze()
        return@withLock newEntry
      } else {
        // Capacity is reached — wait for the next available entry.
        return@withLock loopForConditionalResult {
          // Reload the list, since the thread can be suspended here while the list of entries has been modified.
          val innerEntries = entriesRef.value ?: throw ClosedMultiPoolException
          innerEntries.firstOrNull { it.tryToAcquire() }
        }
      }
    }

    return nextAvailable.asBorrowed(poolLock)
  }

  fun <R> access(action: (T) -> R): R {
    val borrowed = borrowEntry()
    return try {
      action(borrowed.value)
    } finally {
      borrowed.release()
    }
  }

  fun close() {
    if (!poolLock.close())
      return

    val entries = entriesRef.value
    val done = entriesRef.compareAndSet(entries, null)
    check(done)

    entries?.forEach { it.value.close() }
  }

  inner class Entry(val value: T) {
    val isAvailable = AtomicBoolean(true)

    init { freeze() }

    fun tryToAcquire(): Boolean = isAvailable.compareAndSet(expected = true, new = false)

    fun asBorrowed(poolLock: PoolLock): Borrowed<T> = object : Borrowed<T> {
      override val value: T
        get() = this@Entry.value

      override fun release() {
        /**
         * Mark-as-available should be done before signalling blocked threads via [PoolLock.notifyConditionChanged],
         * since the happens-before relationship guarantees the woken thread to see the
         * available entry (if not having been taken by other threads during the wake-up lead time).
         */

        val done = isAvailable.compareAndSet(expected = false, new = true)
        check(done)

        // While signalling blocked threads does not require locking, doing so avoids a subtle race
        // condition in which:
        //
        // 1. a [loopForConditionalResult] iteration in [borrowEntry] slow path is happening concurrently;
        // 2. the iteration fails to see the atomic `isAvailable = true` above;
        // 3. we signal availability here but it is a no-op due to no waiting blocker; and finally
        // 4. the iteration entered an indefinite blocking wait, not being aware of us having signalled availability here.
        //
        // By acquiring the pool lock first, signalling cannot happen concurrently with the loop
        // iterations in [borrowEntry], thus eliminating the race condition.
        poolLock.withLock {
          poolLock.notifyConditionChanged()
        }
      }
    }
  }
}

private val ClosedMultiPoolException get() = IllegalStateException("Attempt to access a closed MultiPool.")
