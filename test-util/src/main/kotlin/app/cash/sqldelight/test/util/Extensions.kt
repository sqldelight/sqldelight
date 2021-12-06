package app.cash.sqldelight.test.util

fun String.withInvariantPathSeparators() = replace("\\", "/")
fun String.withInvariantLineSeparators() = replace("\r\n", "\n")
fun String.splitLines() = split("\\r?\\n".toRegex())
