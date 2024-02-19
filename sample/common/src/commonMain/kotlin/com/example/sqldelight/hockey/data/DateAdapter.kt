package com.example.sqldelight.hockey.data

import app.cash.sqldelight.ColumnAdapter
import kotlinx.datetime.LocalDate

class DateAdapter : ColumnAdapter<LocalDate, Long> {
  override fun decode(databaseValue: Long): LocalDate {
    return LocalDate.fromEpochDays(databaseValue.toInt())
  }

  override fun encode(value: LocalDate): Long {
    return value.toEpochDays().toLong()
  }
}
