package com.example.sqldelight.hockey.data

import app.cash.sqldelight.ColumnAdapter
import java.util.GregorianCalendar

actual typealias Date = GregorianCalendar

actual class DateAdapter actual constructor() : ColumnAdapter<Date, Long> {
  actual override fun encode(value: Date) = value.timeInMillis
  actual override fun decode(databaseValue: Long) = Date.getInstance().apply {
    timeInMillis = databaseValue
  } as Date
}
