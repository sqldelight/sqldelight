package com.example.sqldelight.hockey.data

import com.squareup.sqldelight.ColumnAdapter
import platform.Foundation.*
import kotlin.math.floor

actual class Date internal constructor(internal val nsDate:NSDate){
    actual constructor(year: Int, month: Int, day: Int):this(partsToDate(year, month, day))
}

internal fun partsToDate(year: Int, month: Int, day: Int):NSDate{
    val cal = NSCalendar.currentCalendar
    val comps = NSDateComponents()
    comps.setDay(day.toLong())
    comps.setMonth(month.toLong())
    comps.setYear(year.toLong())
    return cal.dateFromComponents(comps)!!
}

actual class DateAdapter actual constructor() : ColumnAdapter<Date, Long> {
    override fun decode(databaseValue: Long): Date =
        Date(NSDate.dateWithTimeIntervalSince1970(databaseValue.toDouble()/1000))

    override fun encode(value: Date): Long =
        floor(value.nsDate.timeIntervalSince1970).toLong() * 1000L
}