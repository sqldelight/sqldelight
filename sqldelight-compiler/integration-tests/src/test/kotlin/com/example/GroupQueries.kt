package com.example

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.TransacterImpl
import com.squareup.sqldelight.db.SqlDriver
import kotlin.Long
import kotlin.collections.MutableList

class GroupQueries(private val database: TestDatabaseImpl, private val driver: SqlDriver) :
        TransacterImpl(driver) {
    internal val selectAll: MutableList<Query<*>> =
            com.squareup.sqldelight.internal.copyOnWriteList()

    fun selectAll(): Query<Long> = Query(107, selectAll, driver, "SELECT `index` FROM `group`") {
            cursor ->
        cursor.getLong(0)!!
    }
}
