package com.squareup.sqldelight.drivers.native

import co.touchlab.stately.concurrency.AtomicBoolean
import co.touchlab.stately.concurrency.AtomicReference
import co.touchlab.stately.freeze
import com.squareup.sqldelight.db.Closeable
import com.squareup.sqldelight.drivers.native.util.PoolLock

/**
 * A shared pool of connections. Borrowing is blocking when all connections are in use, and the pool has reached its
 * designated capacity.
 */
internal class MultiPool<T : Closeable>(private val capacity: Int, private val producer: () -> T) {
  /**
   * Hold a list of active connections. If it is null, it means the MultiPool has been closed.
   */
  private val entriesRef = AtomicReference<List<Entry>?>(listOf())
  private val poolLock = PoolLock()

  fun borrowEntry(): Borrowed<T> {
    val snapshot = entriesRef.get() ?: throw ClosedMultiPoolException

    // Fastpath: Borrow the first available entry.
    val firstAvailable = snapshot.firstOrNull { it.tryToAcquire() }

    if (firstAvailable != null) {
      return firstAvailable.asBorrowed(poolLock)
    }

    // Slowpath: Create a new entry if capacity limit has not been reached, or wait for the next available entry.
    val nextAvailable = poolLock.withLock {
      // Reload the list since it could've been updated by other threads concurrently.
      val entries = entriesRef.get() ?: throw ClosedMultiPoolException

      if (entries.count() < capacity) {
        // Capacity hasn't been reached — create a new entry to serve this call.
        val newEntry = Entry(producer())
        val done = newEntry.tryToAcquire()
        check(done)

        entriesRef.set(entries + listOf(newEntry))
        return@withLock newEntry
      } else {
        // Capacity is reached — wait for the next available entry.
        return@withLock loopUntilAvailableResult {
          // Reload the list, since the thread can be suspended here while the list of entries has been modified.
          val innerEntries = entriesRef.get() ?: throw ClosedMultiPoolException
          innerEntries.firstOrNull { it.tryToAcquire() }
            .apply { println("MultiPool loopUntilAvailableResult ${this}") }
        }
      }
    }

    return nextAvailable.asBorrowed(poolLock)
  }

  fun <R> access(action: (T) -> R): R {
    val borrowed = borrowEntry()
    val result = action(borrowed.value)
    borrowed.release()
    return result
  }

  fun close() {
    if (!poolLock.close())
      return

    val entries = entriesRef.get()
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

      override fun release() = poolLock.withLock {
        // While acquiring the entry is lock-free, we mark the entry as available inside this [PoolLock] critical
        // section, so that the `loopUntilAvailableResult` blocks can deterministically see available entries.

        val done = isAvailable.compareAndSet(expected = false, new = true)
        check(done)
        signalAvailability()
      }
    }
  }

  interface Borrowed<T> {
    val value: T
    fun release()
  }
}

private val ClosedMultiPoolException get() = IllegalStateException("Attempt to access a closed MultiPool.")
