package com.example.sqldelight.hockey

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

/**
 * Init driver for each platform. Should *always* be called to setup test
 */
expect suspend fun createDriver()

/**
 * Close driver for each platform. Should *always* be called to tear down test
 */
expect suspend fun closeDriver()

/**
 * Platform specific access to HockeyDb
 */
expect fun getDb(): HockeyDb

@OptIn(ExperimentalCoroutinesApi::class)
fun testing(block: suspend CoroutineScope.(HockeyDb) -> Unit) = runTest {
  createDriver()
  block(getDb())
  closeDriver()
}
