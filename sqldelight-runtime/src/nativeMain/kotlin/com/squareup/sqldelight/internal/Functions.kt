package com.squareup.sqldelight.internal

import kotlin.native.concurrent.freeze
import co.touchlab.stately.collections.SharedSet
import co.touchlab.stately.concurrency.Lock
import co.touchlab.stately.concurrency.withLock
import co.touchlab.stately.concurrency.AtomicReference
import co.touchlab.stately.concurrency.value

actual fun <T> safeLazy(initializer: () -> T): Lazy<T> = AtomicFrozenLazy(initializer)

internal class AtomicFrozenLazy<T>(private val initializer: () -> T): Lazy<T>{
    override val value: T
        get() = lock.withLock {
            if(valAtomic.value == null){
                valAtomic.value = initializer().freeze()
            }
            valAtomic.value!!
        }

    override fun isInitialized(): Boolean = valAtomic.value != null

    private val valAtomic = AtomicReference<T?>(null)
    private val lock = Lock()

}

actual fun <T> sharedSet(): MutableSet<T> = SharedSet<T>()