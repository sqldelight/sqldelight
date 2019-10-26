package com.squareup.sqldelight.drivers.sqljs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.promise

fun CoroutineScope.runTest(block: suspend () -> Unit): dynamic = promise { block() }
