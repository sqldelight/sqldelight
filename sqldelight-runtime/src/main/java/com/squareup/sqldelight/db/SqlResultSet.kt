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
package com.squareup.sqldelight.db

interface SqlResultSet {
  fun next(): Boolean
  fun getString(index: Int): String?
  fun getLong(index: Int): Long?
  fun getBytes(index: Int): ByteArray?
  fun getDouble(index: Int): Double?
  fun close()
}

inline fun <R> SqlResultSet.use(block: (SqlResultSet) -> R): R {
  var closed = false
  try {
    return block(this)
  } catch (e: Exception) {
    closed = true
    try {
      close()
    } catch (closeException: Exception) {

    }
    throw e
  } finally {
    if (!closed) {
      close()
    }
  }
}
