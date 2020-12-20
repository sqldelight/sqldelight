package app.cash.sqldelight.adapter.primitive

import kotlin.test.Test
import kotlin.test.assertEquals

class FloatColumnAdapterTest {
  @Test fun decode() {
    assertEquals(10.0f, FloatColumnAdapter.decode(10.0), 1e-6f)
  }

  @Test fun encode() {
    assertEquals(10.7, FloatColumnAdapter.encode(10.7f), 1e-6)
  }
}
