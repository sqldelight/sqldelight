package com.example.sqldelight.hockey.data

import com.squareup.sqldelight.ColumnAdapter

expect class Date(year: Int, month: Int, day: Int)

expect class DateAdapter() : ColumnAdapter<Date, Long>
