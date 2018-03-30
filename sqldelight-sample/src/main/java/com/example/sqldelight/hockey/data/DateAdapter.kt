package com.example.sqldelight.hockey.data

import com.squareup.sqldelight.ColumnAdapter
import java.util.Calendar

class DateAdapter : ColumnAdapter<Calendar, Long> {
  override fun encode(value: Calendar) = value.timeInMillis
  override fun decode(databaseValue: Long) = Calendar.getInstance().apply {
    timeInMillis = databaseValue
  }
}
