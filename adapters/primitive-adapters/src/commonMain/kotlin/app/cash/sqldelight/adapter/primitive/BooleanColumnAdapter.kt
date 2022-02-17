package app.cash.sqldelight.adapter.primitive

import app.cash.sqldelight.ColumnAdapter

object BooleanColumnAdapter : ColumnAdapter<Boolean, Long> {
  override fun decode(databaseValue: Long): Boolean = when (databaseValue) {
    1L -> true
    0L -> false
    else -> error("Value from database is $databaseValue but expected 0 or 1.")
  }

  override fun encode(value: Boolean): Long = if (value) 1L else 0L
}
