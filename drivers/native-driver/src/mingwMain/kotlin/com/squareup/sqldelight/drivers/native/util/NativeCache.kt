package com.squareup.sqldelight.drivers.native.util

import co.touchlab.stately.collections.SharedHashMap
import co.touchlab.stately.collections.frozenHashMap

internal actual fun <T : Any> nativeCache(): NativeCache<T> =
  NativeCacheImpl()

private class NativeCacheImpl<T : Any> : NativeCache<T> {
  private val dictionary = frozenHashMap<String, T?>() as SharedHashMap<String, T?>

  override fun put(key: String, value: T?): T? = dictionary.put(key, value)
  override fun getOrCreate(key: String, block: () -> T): T = dictionary.getOrPut(key) { block() }!!
  override fun remove(key: String): T? = dictionary.remove(key)
  override fun cleanUp(block: (T) -> Unit) {
    dictionary.values.forEach { v ->
      v?.let { block(it) }
    }
  }
}
