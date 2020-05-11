package com.example.sqldelight.hockey.platform

import com.example.sqldelight.hockey.data.Date

expect class DateFormatHelper(format: String) {
  fun format(d: Date): String
}

val defaultFormatter = DateFormatHelper("dd/MM/yyyy")
