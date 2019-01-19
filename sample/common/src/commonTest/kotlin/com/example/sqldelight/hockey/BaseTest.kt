package com.example.sqldelight.hockey

import kotlin.test.BeforeTest
import kotlin.test.Test

open class BaseTest {
  @BeforeTest
  fun initDb() {
    createDriver()
  }

  @Test
  fun closeDb() {
    closeDriver()
  }


}

/**
 * Init driver for each platform. Should *always* be called to setup test
 */
expect fun createDriver()

/**
 * Close driver for each platform. Should *always* be called to tear down test
 */
expect fun closeDriver()

/**
 * Platform specific access to HockeyDb
 */
expect fun BaseTest.getDb(): HockeyDb

