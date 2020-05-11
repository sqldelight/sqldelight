package com.squareup.sqldelight.internal

import java.util.concurrent.atomic.AtomicReference

actual typealias AtomicBoolean = java.util.concurrent.atomic.AtomicBoolean

actual typealias Atomic<V> = AtomicReference<V>
