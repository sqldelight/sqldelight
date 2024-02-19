package com.example.sqldelight.hockey.platform

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

actual class DateFormatHelper actual constructor(format: String) {
  val dateFormatter = object : ThreadLocal<DateFormat>() {
    override fun initialValue(): DateFormat = SimpleDateFormat(format)
  }

  actual fun format(d: LocalDate): String {
    val epochMilliseconds = d.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    return dateFormatter.get()!!.format(Date(epochMilliseconds))
  }
}
