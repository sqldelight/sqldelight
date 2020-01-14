package com.squareup.sqldelight.internal

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

internal fun presizeArguments(
  count: Int,
  offset: Int
): Int {
  var size = 0
  var currentBase = 0
  var pow: Int = 10f.pow(currentBase).roundToInt()
  var lastPow: Int
  while (offset + count > pow) {
    lastPow = pow
    pow = 10f.pow(++currentBase).roundToInt()
    size += (currentBase + 1) * (min(pow, offset + count) - max(lastPow, min(pow, offset)))
  }
  return size + 2 + (count-1)*2
}