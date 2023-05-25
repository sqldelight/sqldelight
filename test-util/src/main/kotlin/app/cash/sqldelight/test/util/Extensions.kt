package app.cash.sqldelight.test.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

fun String.withInvariantPathSeparators() = replace("\\", "/")
fun String.withInvariantLineSeparators() = replace("\r\n", "\n")
fun String.splitLines() = split("\\r?\\n".toRegex())

private val df = DecimalFormat(
  "###,##0",
  DecimalFormatSymbols().apply {
    decimalSeparator = '.'
    groupingSeparator = '_'
  },
)

val Int.withUnderscores: String get() = df.format(this)
