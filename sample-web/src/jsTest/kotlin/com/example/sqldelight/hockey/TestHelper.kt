package com.example.sqldelight.hockey

import com.example.sqldelight.hockey.data.DbHelper
import kotlinx.coroutines.test.runTest

fun testDb(block: suspend (database: HockeyDb) -> Unit) = runTest {
  val helper = DbHelper()
  helper.withDatabase(block)
}
