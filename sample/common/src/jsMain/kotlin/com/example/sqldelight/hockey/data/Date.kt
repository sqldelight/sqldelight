package com.example.sqldelight.hockey.data

import com.squareup.sqldelight.ColumnAdapter

actual typealias Date = kotlin.js.Date

actual class DateAdapter actual constructor() : ColumnAdapter<Date, Long> {
  override fun encode(value: Date) = value.getTime().toLong()
  override fun decode(databaseValue: Long) = Date(databaseValue)
}
