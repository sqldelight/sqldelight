package com.squareup.sqlite.migrations

interface DatabaseDiff {
  fun isEmpty(): Boolean
  fun printTo(out: Appendable)
}
