package com.squareup.sqldelight

import kotlin.test.Test
import kotlin.test.assertEquals

class FakeTest {
  @Test fun testSomeShitSucceeds() {
    assertEquals(1, 1, "Nice job")
  }

  @Test fun testSomeShitFails() {
    assertEquals(1, 0, "Fuck you fucked up")
  }
}