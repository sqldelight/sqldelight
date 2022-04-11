/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.sqldelight.coroutines

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.use
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun <T : Any> Query<T>.assert(body: QueryAssert.() -> Unit) {
  execute().use { cursor ->
    QueryAssert(cursor).apply(body)
    val remainingRows = mutableListOf<String>()
    while (cursor.next()) {
      remainingRows += buildString {
        try {
          for (i in 0..Int.MAX_VALUE) {
            val columnValue = cursor.getString(i)
            if (i > 0) {
              append('\t')
            }
            append(columnValue)
          }
        } catch (e: Exception) {
        }
      }
    }
    assertEquals(emptyList<String>(), remainingRows, "remaining cursor rows")
  }
}

class QueryAssert(private val cursor: SqlCursor) {
  private var row = 0

  fun hasRow(vararg values: String) {
    row += 1

    assertTrue(cursor.next(), "row $row does not exist")
    for (i in values.indices) {
      assertEquals(values[i], cursor.getString(i), "row $row column '$i'")
    }
  }
}
