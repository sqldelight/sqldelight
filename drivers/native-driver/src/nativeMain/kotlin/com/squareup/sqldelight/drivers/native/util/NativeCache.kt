package com.squareup.sqldelight.drivers.native.util

internal interface NativeCache<T : Any> {
  fun put(key: String, value: T?): T?
  fun getOrCreate(key: String, block: () -> T): T
  fun remove(key: String): T?
  fun cleanUp(block: (T) -> Unit)
}

internal expect fun <T : Any> nativeCache(): NativeCache<T>