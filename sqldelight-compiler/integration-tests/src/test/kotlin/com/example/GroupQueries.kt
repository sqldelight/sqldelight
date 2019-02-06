package com.example

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlDriver
import kotlin.Long
import kotlin.collections.MutableList

class GroupQueries(private val database: TestDatabase, private val driver: SqlDriver) :
        Transacter(driver) {
    internal val selectAll: MutableList<Query<*>> =
            com.squareup.sqldelight.internal.copyOnWriteList()

    fun selectAll(): Query<Long> = Query(107, selectAll, driver, "SELECT `index` FROM `group`") {
            cursor ->
        cursor.getLong(0)!!
    }
}
