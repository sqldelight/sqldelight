package com.example.sqldelight.hockey.data

import app.cash.sqldelight.ColumnAdapter

expect class Date(year: Int, month: Int, day: Int)

expect class DateAdapter() : ColumnAdapter<Date, Long> {
  override fun encode(value: Date): Long
  override fun decode(databaseValue: Long): Date
}
