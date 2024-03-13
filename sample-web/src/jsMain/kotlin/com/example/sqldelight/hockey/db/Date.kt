package com.example.sqldelight.hockey.db

import kotlinx.datetime.internal.JSJoda.LocalDate

actual typealias Date = LocalDate

actual fun Date(year: Int, month: Int, day: Int): Date = Date.of(year, month, day)

actual fun dateFromEpochDays(days: Int): Date = Date.ofEpochDay(days)
