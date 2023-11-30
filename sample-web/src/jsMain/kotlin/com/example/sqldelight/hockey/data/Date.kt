package com.example.sqldelight.hockey.data

import app.cash.sqldelight.ColumnAdapter
import kotlin.js.Date

actual typealias Date = kotlin.js.Date

actual class DateAdapter : ColumnAdapter<Date, Long> {
  override fun encode(value: Date) = value.getTime().toLong()
  override fun decode(databaseValue: Long) = Date(databaseValue)
}
