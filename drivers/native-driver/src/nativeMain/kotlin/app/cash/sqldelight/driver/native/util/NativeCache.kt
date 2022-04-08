package app.cash.sqldelight.driver.native.util

internal class MutableCache<T : Any> {
  private val dictionary = mutableMapOf<String, T>()
  private val lock = PoolLock()

  fun put(key: String, value: T?): T? = lock.withLock {
    if (value == null) {
      dictionary.remove(key)
    } else {
      dictionary.put(key, value)
    }
  }

  fun getOrCreate(key: String, block: () -> T): T = lock.withLock {
    dictionary.getOrPut(key, block)
  }

  fun remove(key: String): T? = lock.withLock {
    dictionary.remove(key)
  }

  fun cleanUp(block: (T) -> Unit) = lock.withLock {
    dictionary.values.forEach(block)
  }
}
