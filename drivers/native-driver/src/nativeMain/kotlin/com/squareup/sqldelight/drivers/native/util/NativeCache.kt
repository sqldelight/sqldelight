package com.squareup.sqldelight.drivers.native.util

internal interface NativeCach<T : Any> {
  fun put(key: String, value: T?): T?
  fun getOrCreate(key: String, block: () -> T): T
  fun remove(key: String): T?
  fun cleanUp(block: (T) -> Unit)
}

internal expect fun <T : Any> nativeCache(): NativeCach<T>