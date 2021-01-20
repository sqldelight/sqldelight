package com.squareup.sqldelight.drivers.native.util

import platform.Foundation.NSMutableDictionary
import platform.Foundation.allValues
import platform.Foundation.setValue
import platform.Foundation.valueForKey
import kotlin.native.concurrent.freeze

internal actual fun <T:Any> nativeCache():NativeCach<T> =
    NativeCacheImpl<T>()

internal class NativeCacheImpl<T:Any>: NativeCach<T> {
    private val dictionary = NSMutableDictionary()
    private val lock = PoolLock()

    override fun put(key: String, value: T?):T? {
        value.freeze()
        return lock.withLock {
            val r = dictionary.valueForKey(key)
            dictionary.setValue(value, key)
            r as? T
        }
    }

    override fun getOrCreate(key: String, block:()->T):T = lock.withLock {
        val r = dictionary.valueForKey(key) as? T
        r ?: block().apply {
            freeze()
            dictionary.setValue(this, key)
        }
    }

    override fun remove(key: String):T? = lock.withLock {
        val r = dictionary.valueForKey(key)
        dictionary.removeObjectForKey(key)
        r as? T
    }

    override fun cleanUp(block: (T) -> Unit) = lock.withLock {
        dictionary.allValues.forEach { block(it as T)}
    }
}