package com.example.sqldelight.hockey.data

import com.squareup.sqldelight.ColumnAdapter
import java.util.GregorianCalendar

actual typealias Date = GregorianCalendar

actual class DateAdapter actual constructor() : ColumnAdapter<Date, Long> {
  override fun encode(value: Date) = value.timeInMillis
  override fun decode(databaseValue: Long) = Date.getInstance().apply {
    timeInMillis = databaseValue
  } as Date
}
