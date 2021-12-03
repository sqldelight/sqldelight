package com.example

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.SqlDriver
import kotlin.Long

public class GroupQueries(
  private val driver: SqlDriver
) : TransacterImpl(driver) {
  public fun selectAll(): Query<Long> = Query(165688501, arrayOf("group"), driver, "Group.sq",
      "selectAll", "SELECT `index` FROM `group`") { cursor ->
    cursor.getLong(0)!!
  }
}
