package app.cash.sqldelight.intellij.run

internal data class SqlParameter(
  val name: String,
  val value: String = "",
  val range: IntRange
)
