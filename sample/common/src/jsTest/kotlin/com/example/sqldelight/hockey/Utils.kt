package com.example.sqldelight.hockey

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.promise

fun CoroutineScope.runTest(block: suspend () -> Unit): dynamic = promise { block() }
