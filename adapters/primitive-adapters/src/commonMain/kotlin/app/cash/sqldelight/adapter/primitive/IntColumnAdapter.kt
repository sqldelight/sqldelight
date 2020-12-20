package app.cash.sqldelight.adapter.primitive

import app.cash.sqldelight.ColumnAdapter

object IntColumnAdapter : ColumnAdapter<Int, Long> {
  override fun decode(databaseValue: Long): Int = databaseValue.toInt()

  override fun encode(value: Int): Long = value.toLong()
}
