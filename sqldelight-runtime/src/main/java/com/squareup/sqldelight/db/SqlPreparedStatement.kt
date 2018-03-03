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

interface SqlPreparedStatement {
  fun bindBytes(index: Int, bytes: ByteArray?)
  fun bindLong(index: Int, long: Long?)
  fun bindDouble(index: Int, double: Double?)
  fun bindString(index: Int, string: String?)

  fun executeQuery(): SqlResultSet
  fun execute(): Int
}
