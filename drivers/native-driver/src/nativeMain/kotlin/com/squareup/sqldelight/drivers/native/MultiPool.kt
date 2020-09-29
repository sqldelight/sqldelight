package com.squareup.sqldelight.drivers.native

import co.touchlab.stately.concurrency.AtomicInt
import co.touchlab.stately.concurrency.AtomicReference
import co.touchlab.stately.concurrency.Lock
import co.touchlab.stately.concurrency.withLock
import com.squareup.sqldelight.db.Closeable
import kotlin.native.concurrent.freeze

/**
 * A shared pool of connections. Borrowing is blocking when all connections are in use, and the pool has reached its
 * designated capacity.
 */
internal class MultiPool<T : Closeable>(private val capacity: Int, private val producer: () -> T) {
  /**
   * Hold a list of active connections. If it is null, it means the MultiPool has been closed.
   */
  private val entriesRef = AtomicReference<List<Entry>?>(listOf())
  private val poolUpdateLock = Lock()

  @OptIn(ExperimentalUnsignedTypes::class)
  fun borrowEntry(): Borrowed<T> {
    val availableEntries = entriesRef.get() ?: throw ClosedMultiPoolException

    // Fastpath: Borrow the first available entry.
    for (entry in availableEntries) {
      val borrowed = entry.tryToBorrow()

      if (borrowed != null) {
        return borrowed
      }
    }

    // Slowpath: Wait on the entry with the lowest wait count.
    val entryToWait: Entry = poolUpdateLock.withLock {
      // Refetch the list since it could've been updated by others.
      val entries = entriesRef.get() ?: throw ClosedMultiPoolException

      if (entries.count() < capacity) {
        // Capacity hasn't been reached — create a new entry to serve this call.
        val newEntry = Entry(producer()).freeze()
        entriesRef.set(entries + listOf(newEntry))
        return@withLock newEntry
      } else {
        // Capacity is reached — find an existing entry that is likely to be available soon.
        val entry = entries.minByOrNull { it.waitCount.get() } ?: throw ClosedMultiPoolException
        entry.waitCount.incrementAndGet()
        return@withLock entry
      }
    }

    val borrowed = entryToWait.waitAndBorrow()
    entryToWait.waitCount.decrementAndGet()
    return borrowed
  }

  fun <R> access(action: (T) -> R): R {
    val borrowed = borrowEntry()
    val result = action(borrowed.value)
    borrowed.release()
    return result
  }

  fun close() = poolUpdateLock.withLock {
    var entries = entriesRef.get()
    while (entries != null && !entriesRef.compareAndSet(entries, null)) {
      entries = entriesRef.get()
    }

    entries?.forEach { it.value.close() }
  }

  inner class Entry(override val value: T) : Borrowed<T> {
    val waitCount = AtomicInt(0)
    private val lock = Lock()

    fun waitAndBorrow(): Borrowed<T> {
      lock.lock()
      return this
    }

    fun tryToBorrow(): Borrowed<T>? {
      if (lock.tryLock()) {
        return this
      }

      return null
    }

    override fun release() = lock.unlock()
  }

  interface Borrowed<T> {
    val value: T
    fun release()
  }
}

private val ClosedMultiPoolException get() = IllegalStateException("Attempt to access a closed MultiPool.")
