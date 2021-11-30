/*
 * Copyright (C) 2018 Square, Inc.
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
package app.cash.sqldelight.db

/**
 * Represents a SQL result set which can be iterated through with [next]. Initially the cursor will
 * not point to any row, and calling [next] once will iterate to the first row.
 */
interface SqlCursor : Closeable {
  /**
   * Move to the next row in the result set.
   *
   * @return true if the cursor successfully moved to a new row, false if there was no row to
   *   iterate to.
   */
  fun next(): Boolean

  /**
   * @return The string or null value of column [index] for the current row of the result set.
   */
  fun getString(index: Int): String?

  /**
   * @return The int or null value of column [index] for the current row of the result set.
   */
  fun getLong(index: Int): Long?

  /**
   * @return The bytes or null value of column [index] for the current row of the result set.
   */
  fun getBytes(index: Int): ByteArray?

  /**
   * @return The double or null value of column [index] for the current row of the result set.
   */
  fun getDouble(index: Int): Double?
}
