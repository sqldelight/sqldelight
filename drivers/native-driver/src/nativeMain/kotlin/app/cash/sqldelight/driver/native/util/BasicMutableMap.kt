package app.cash.sqldelight.driver.native.util

import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze

/**
 * Basic map functionality, implemented differently by memory model. Both are safe to use with
 * multiple threads.
 */
internal interface BasicMutableMap<K : Any, V : Any> {
  fun getOrPut(key: K, block: () -> V): V
  fun put(key: K, value: V)
  fun get(key: K): V?
  fun remove(key: K)
  val keys: Collection<K>
  val values: Collection<V>
  fun cleanUp(block: (V) -> Unit)
}

internal fun <K : Any, V : Any> basicMutableMap(): BasicMutableMap<K, V> =
  if (Platform.memoryModel == MemoryModel.STRICT) {
    BasicMutableMapStrict()
  } else {
    BasicMutableMapShared()
  }

/**
 * New memory model only. May be able to remove the lock or use a more optimized version of a concurrent
 * map, but relative to where it's being used, there isn't likely to be much of a performance hit.
 */
private class BasicMutableMapShared<K : Any, V : Any> : BasicMutableMap<K, V> {
  private val lock = PoolLock(reentrant = true)
  private val data = mutableMapOf<K, V>()

  override fun getOrPut(key: K, block: () -> V): V = lock.withLock { data.getOrPut(key, block) }

  override val values: Collection<V>
    get() = lock.withLock { data.values }

  override fun put(key: K, value: V) {
    lock.withLock { data[key] = value }
  }

  override fun get(key: K): V? = lock.withLock { data[key] }

  override fun remove(key: K) {
    lock.withLock { data.remove(key) }
  }

  override val keys: Collection<K>
    get() = lock.withLock { data.keys }

  override fun cleanUp(block: (V) -> Unit) {
    lock.withLock {
      data.values.forEach(block)
    }
  }
}

/**
 * Slow, but compatible with the strict memory model.
 */
private class BasicMutableMapStrict<K : Any, V : Any> : BasicMutableMap<K, V> {
  private val lock = PoolLock(reentrant = true)
  private val mapReference = AtomicReference(mutableMapOf<K, V>().freeze())

  override fun getOrPut(key: K, block: () -> V): V = lock.withLock {
    val result = mapReference.value[key]
    if (result == null) {
      val created = block()
      _put(key, created)
      created
    } else {
      result
    }
  }

  override val values: Collection<V>
    get() = lock.withLock { mapReference.value.values }

  override fun put(key: K, value: V) {
    lock.withLock {
      _put(key, value)
    }
  }

  private fun _put(key: K, value: V) {
    mapReference.value = mutableMapOf(
      Pair(key, value),
      *mapReference.value.map { entry ->
        Pair(entry.key, entry.value)
      }.toTypedArray(),
    ).freeze()
  }

  override fun get(key: K): V? = lock.withLock { mapReference.value[key] }

  override fun remove(key: K) {
    lock.withLock {
      val sourceMap = mapReference.value
      val resultMap = mutableMapOf<K, V>()
      sourceMap.keys.subtract(listOf(key)).forEach { key ->
        val value = sourceMap[key]
        if (value != null) {
          resultMap[key] = value
        }
      }
      mapReference.value = resultMap.freeze()
    }
  }

  override val keys: Collection<K>
    get() = lock.withLock { mapReference.value.keys }

  override fun cleanUp(block: (V) -> Unit) {
    lock.withLock {
      mapReference.value.values.forEach(block)
    }
  }
}
