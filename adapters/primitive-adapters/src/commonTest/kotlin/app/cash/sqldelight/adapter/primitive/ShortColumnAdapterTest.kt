package app.cash.sqldelight.adapter.primitive

import kotlin.test.Test
import kotlin.test.assertEquals

class ShortColumnAdapterTest {
  @Test fun decode() {
    assertEquals(10, ShortColumnAdapter.decode(10))
  }

  @Test fun encode() {
    assertEquals(10, ShortColumnAdapter.encode(10))
  }
}
