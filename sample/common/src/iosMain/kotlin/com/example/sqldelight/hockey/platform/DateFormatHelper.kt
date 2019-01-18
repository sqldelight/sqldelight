package com.example.sqldelight.hockey.platform

import com.example.sqldelight.hockey.data.Date
import platform.Foundation.NSDateFormatter

actual class DateFormatHelper actual constructor(format: String) {
  private val formatter: NSDateFormatter

  init {
    formatter = NSDateFormatter()
    formatter.dateFormat = format
  }

  actual fun format(d: Date): String = formatter.stringFromDate(d.nsDate)
}