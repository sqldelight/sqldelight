package com.example.sqldelight.hockey.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

class DateFormatHelperTest {
  @Test
  fun format() {
    val df = DateFormatHelper("yyyy-MM-dd")
    val date = LocalDate(2019, 4, 7)
    val formatted = df.format(date)

    assertEquals("2019-04-07", formatted)
  }
}
