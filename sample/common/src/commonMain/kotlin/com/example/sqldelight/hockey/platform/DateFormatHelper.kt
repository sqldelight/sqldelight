package com.example.sqldelight.hockey.platform

import kotlinx.datetime.LocalDate

expect class DateFormatHelper(format: String) {
  fun format(d: LocalDate): String
}

val defaultFormatter = DateFormatHelper("dd/mm/yyyy")
