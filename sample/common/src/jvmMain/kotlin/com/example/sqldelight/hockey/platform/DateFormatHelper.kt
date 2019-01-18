package com.example.sqldelight.hockey.platform

import com.example.sqldelight.hockey.data.Date
import java.text.DateFormat
import java.text.SimpleDateFormat

actual class DateFormatHelper actual constructor(format: String) {
  val dateFormatter = object : ThreadLocal<DateFormat>() {
    override fun initialValue(): DateFormat = SimpleDateFormat(format)
  }

  actual fun format(d: Date): String = dateFormatter.get()!!.format(d.time)
}