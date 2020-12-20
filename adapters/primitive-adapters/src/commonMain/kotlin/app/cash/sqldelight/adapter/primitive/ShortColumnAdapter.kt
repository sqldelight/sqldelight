package app.cash.sqldelight.adapter.primitive

import app.cash.sqldelight.ColumnAdapter

object ShortColumnAdapter : ColumnAdapter<Short, Long> {
  override fun decode(databaseValue: Long): Short = databaseValue.toShort()

  override fun encode(value: Short): Long = value.toLong()
}
