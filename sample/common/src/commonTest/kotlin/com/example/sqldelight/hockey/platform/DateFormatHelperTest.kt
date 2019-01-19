package com.example.sqldelight.hockey.platform

import com.example.sqldelight.hockey.data.Date
import kotlin.test.Test
import kotlin.test.assertEquals

class DateFormatHelperTest {
  @Test
  fun format(){
    val df = DateFormatHelper("yyyy-MM-dd")
    val date = Date(2019, 3, 7)
    val formatted = df.format(date)

    assertEquals("2019-04-07", formatted)
  }
}
