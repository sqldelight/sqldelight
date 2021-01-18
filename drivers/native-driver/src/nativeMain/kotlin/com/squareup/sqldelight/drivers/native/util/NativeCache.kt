package com.squareup.sqldelight.drivers.native.util


import platform.Foundation.NSMutableDictionary
import platform.Foundation.allValues
import platform.Foundation.setValue
import platform.Foundation.valueForKey
import kotlin.native.concurrent.freeze

class NativeCache<T:Any> {
    private val dictionary = NSMutableDictionary()
    private val lock = PoolLock()

    fun put(key: String, value: T?):T? {
        value.freeze()
        return lock.withLock {
            val r = dictionary.valueForKey(key)
            dictionary.setValue(value, key)
            r as? T
        }
    }

    fun getOrCreate(key: String, block:()->T):T = lock.withLock {
        val r = dictionary.valueForKey(key) as? T
        r ?: block().apply {
            freeze()
            dictionary.setValue(this, key)
        }
    }

    fun remove(key: String):T? = lock.withLock {
        val r = dictionary.valueForKey(key)
        dictionary.removeObjectForKey(key)
        r as? T
    }

    fun cleanUp(block: (T) -> Unit) = lock.withLock {
        dictionary.allValues.forEach { block(it as T)}
    }
}