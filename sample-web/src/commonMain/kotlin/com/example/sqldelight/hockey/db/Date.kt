package com.example.sqldelight.hockey.db

import app.cash.sqldelight.ColumnAdapter

expect class Date {
  fun toEpochDay(): Double
}

expect fun Date(year: Int, month: Int, day: Int): Date
expect fun dateFromEpochDays(days: Int): Date

class DateAdapter : ColumnAdapter<Date, Long> {
  override fun encode(value: Date) = value.toEpochDay().toLong()
  override fun decode(databaseValue: Long) = dateFromEpochDays(databaseValue.toInt())
}
