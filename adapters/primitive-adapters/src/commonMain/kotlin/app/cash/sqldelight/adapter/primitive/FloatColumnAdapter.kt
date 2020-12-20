package app.cash.sqldelight.adapter.primitive

import app.cash.sqldelight.ColumnAdapter

object FloatColumnAdapter : ColumnAdapter<Float, Double> {
  override fun decode(databaseValue: Double): Float = databaseValue.toFloat()

  override fun encode(value: Float): Double = value.toDouble()
}
