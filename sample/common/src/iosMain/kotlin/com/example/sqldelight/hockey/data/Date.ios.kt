package com.example.sqldelight.hockey.data

import app.cash.sqldelight.ColumnAdapter
import kotlin.math.floor
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970

actual class Date internal constructor(internal val nsDate: NSDate) {
  actual constructor(year: Int, month: Int, day: Int) : this(partsToDate(year, month + 1, day))
}

internal fun partsToDate(year: Int, month: Int, day: Int): NSDate {
  val cal = NSCalendar.currentCalendar
  val comps = NSDateComponents()
  comps.setDay(day.toLong())
  comps.setMonth(month.toLong())
  comps.setYear(year.toLong())
  return cal.dateFromComponents(comps)!!
}

actual class DateAdapter actual constructor() : ColumnAdapter<Date, Long> {
  actual override fun decode(databaseValue: Long): Date =
    Date(NSDate.dateWithTimeIntervalSince1970(databaseValue.toDouble() / 1000))

  actual override fun encode(value: Date): Long =
    floor(value.nsDate.timeIntervalSince1970 * 1000L).toLong()
}
