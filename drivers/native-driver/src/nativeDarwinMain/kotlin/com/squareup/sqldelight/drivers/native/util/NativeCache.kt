package com.squareup.sqldelight.drivers.native.util

import platform.Foundation.NSMutableDictionary
import platform.Foundation.allValues
import platform.Foundation.setValue
import platform.Foundation.valueForKey
import kotlin.native.concurrent.freeze

internal actual fun <T : Any> nativeCache(): NativeCache<T> =
  NativeCacheImpl()

private class NativeCacheImpl<T : Any> : NativeCache<T> {
  private val dictionary = NSMutableDictionary()
  private val lock = PoolLock()

  @Suppress("UNCHECKED_CAST")
  override fun put(key: String, value: T?): T? {
    value.freeze()
    return lock.withLock {
      val r = dictionary.valueForKey(key)
      dictionary.setValue(value, key)
      r as? T
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun getOrCreate(key: String, block: () -> T): T = lock.withLock {
    val r = dictionary.valueForKey(key) as? T
    r ?: block().apply {
      freeze()
      dictionary.setValue(this, key)
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun remove(key: String): T? = lock.withLock {
    val r = dictionary.valueForKey(key)
    dictionary.removeObjectForKey(key)
    r as? T
  }

  @Suppress("UNCHECKED_CAST")
  override fun cleanUp(block: (T) -> Unit) = lock.withLock {
    dictionary.allValues.forEach { entry ->
      entry?.let { block(entry as T) }
    }
  }
}
