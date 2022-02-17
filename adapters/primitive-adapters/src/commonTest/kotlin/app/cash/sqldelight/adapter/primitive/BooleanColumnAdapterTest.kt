package app.cash.sqldelight.adapter.primitive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BooleanColumnAdapterTest {
  @Test fun decode() {
    assertFalse { BooleanColumnAdapter.decode(0L) }
    assertTrue { BooleanColumnAdapter.decode(1L) }
    assertFailsWith<IllegalStateException> { BooleanColumnAdapter.decode(2L) }
  }

  @Test fun encode() {
    assertEquals(0, BooleanColumnAdapter.encode(false))
    assertEquals(1, BooleanColumnAdapter.encode(true))
  }
}
