package com.squareup.sqlite.migrations

interface DatabaseDiff {
  fun printTo(out: Appendable)
}
