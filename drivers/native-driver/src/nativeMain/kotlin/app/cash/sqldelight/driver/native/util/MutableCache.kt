package app.cash.sqldelight.driver.native.util

import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze

internal class MutableCache<T : Any> {
  private val dictionary: BasicMap<Int, T> = basicConcurrentMap()

  private val lock = PoolLock()

  fun getOrCreate(key: Int, block: () -> T): T = lock.withLock {
    dictionary.getOrPut(key, block)
  }

  fun cleanUp(block: (T) -> Unit) = lock.withLock {
    dictionary.values.forEach(block)
  }
}

internal fun <K : Any, V : Any> basicConcurrentMap(): BasicMap<K, V> =
  if (Platform.memoryModel == MemoryModel.STRICT) {
    BasicMapStrict()
  } else {
    BasicMapMutable()
  }

internal interface BasicMap<K : Any, V : Any> {
  fun getOrPut(key: K, block: () -> V): V
  fun put(key: K, value: V)
  fun get(key: K): V?
  fun remove(key: K)
  val keys: Collection<K>
  val values: Collection<V>
}

/**
 * New memory model only.
 */
private class BasicMapMutable<K : Any, V : Any> : BasicMap<K, V> {
  private val data = mutableMapOf<K, V>()

  override fun getOrPut(key: K, block: () -> V): V = data.getOrPut(key, block)

  override val values: Collection<V>
    get() = data.values

  override fun put(key: K, value: V) {
    data[key] = value
  }

  override fun get(key: K): V? = data[key]

  override fun remove(key: K) {
    data.remove(key)
  }

  override val keys: Collection<K>
    get() = data.keys
}

/**
 * Slow, but compatible with the strict memory model
 */
private class BasicMapStrict<K : Any, V : Any> : BasicMap<K, V> {
  private val mapReference = AtomicReference(mutableMapOf<K, V>().freeze())

  override fun getOrPut(key: K, block: () -> V): V {
    val result = mapReference.value[key]
    return if (result == null) {
      val created = block()
      put(key, created)
      created
    } else {
      result
    }
  }

  override val values: Collection<V>
    get() = mapReference.value.values

  override fun put(key: K, value: V) {
    mapReference.value = mutableMapOf(
      Pair(key, value),
      *mapReference.value.map { entry ->
        Pair(entry.key, entry.value)
      }.toTypedArray()
    ).freeze()
  }

  override fun get(key: K): V? = mapReference.value[key]

  override fun remove(key: K) {
    val sourceMap = mapReference.value
    val resultMap = mutableMapOf<K, V>()
    sourceMap.keys.subtract(listOf(key)).forEach { key ->
      val value = sourceMap[key]
      if (value != null)
        resultMap[key] = value
    }
    mapReference.value = resultMap.freeze()
  }

  override val keys: Collection<K>
    get() = mapReference.value.keys
}
