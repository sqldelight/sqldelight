package app.cash.sqlite.migrations

interface DatabaseDiff {
  fun printTo(out: Appendable)
}
