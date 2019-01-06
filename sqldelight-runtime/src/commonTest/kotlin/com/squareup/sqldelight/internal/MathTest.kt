package com.squareup.sqldelight.internal

import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

class MathTest {
  @JsName("single")
  @Test fun `presizing works for single`() {
    assertEquals("(?1)".length, presizeArguments(1, 1))
  }

  @JsName("upToTen")
  @Test fun `presizing works for up to 10`() {
    assertEquals("(?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10)".length, presizeArguments(10, 1))
  }

  @JsName("greaterThanTen")
  @Test fun `presizing works for numbers greater than 10`() {
    assertEquals("(?12, ?13, ?14)".length, presizeArguments(3, 12))
  }

  @JsName("SpanOneHundred")
  @Test fun `presizing works for numbers that span 100`() {
    assertEquals("(?98, ?99, ?100, ?101, ?102)".length, presizeArguments(5, 98))
  }
}