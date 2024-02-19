package com.example.sqldelight.hockey.platform

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toNSDateComponents
import platform.Foundation.NSDateFormatter

actual class DateFormatHelper actual constructor(format: String) {
  private val formatter: NSDateFormatter = NSDateFormatter()

  init {
    formatter.dateFormat = format
  }

  actual fun format(d: LocalDate): String = formatter.stringFromDate(d.toNSDateComponents().date!!)
}
