package com.squareup.sqldelight.internal

import co.touchlab.stately.collections.SharedSet

actual fun <T> sharedSet(): MutableSet<T> = SharedSet<T>()