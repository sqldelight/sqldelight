package com.sample

import kotlin.test.Test
import kotlin.test.assertEquals

class TestGreeter {
  @Test fun testPlatform() {
    val greeter = Greeter()
    assertEquals("Hello from jvm", greeter.greeting())
  }
}