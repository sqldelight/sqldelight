package com.squareup.sqldelight.drivers.native.util

import app.cash.sqldelight.driver.native.util.basicMutableMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MutableCacheTest {
  @Test
  fun basicOperations() {
    val map = basicMutableMap<String, SomeData>()
    map.put("abc", SomeData("123"))
    map.put("8675", SomeData("309"))

    assertEquals(map.get("abc"), SomeData("123"))

    map.remove("abc")

    assertNull(map.get("abc"))

    assertEquals(map.keys.toList(), listOf("8675"))
    assertEquals(map.values.toList(), listOf(SomeData("309")))
  }

  @Test
  fun cleanUp() {
    val map = basicMutableMap<String, SomeData>()
    repeat(20) { index ->
      map.put("i$index", SomeData("v$index"))
    }

    var count = 0
    map.cleanUp { count++ }
    assertEquals(count, 20)
  }
}

data class SomeData(val s: String)
