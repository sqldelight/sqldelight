package com.example.sqldelight.hockey.platform

import com.example.sqldelight.hockey.data.Date

@JsModule("dateformat")
external fun dateFormat(date: Date, format: String): String

actual class DateFormatHelper actual constructor(private val format: String) {
  actual fun format(d: Date): String = dateFormat(d, format.replace('M', 'm'))
}
