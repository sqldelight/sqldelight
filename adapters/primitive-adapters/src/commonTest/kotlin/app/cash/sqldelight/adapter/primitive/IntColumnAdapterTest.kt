package app.cash.sqldelight.adapter.primitive

import kotlin.test.Test
import kotlin.test.assertEquals

class IntColumnAdapterTest {
  @Test fun decode() {
    assertEquals(10, IntColumnAdapter.decode(10))
  }

  @Test fun encode() {
    assertEquals(10, IntColumnAdapter.encode(10))
  }
}
