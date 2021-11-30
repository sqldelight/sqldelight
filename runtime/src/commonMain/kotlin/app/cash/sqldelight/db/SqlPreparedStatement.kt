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
 * Represents a SQL statement that has been prepared by a driver to be executed.
 *
 * This type is not thread safe unless otherwise specified by the driver emitting these.
 *
 * Prepared statements should not be cached by client code. Drivers can implement caching by using
 * the integer identifier passed to [SqlDriver.execute] or [SqlDriver.executeQuery].
 * Client code can pass the same identifier to that method to request that the prepared statement
 * is cached.
 */
interface SqlPreparedStatement {

  /**
   * Bind [bytes] to the underlying statement at [index].
   */
  fun bindBytes(index: Int, bytes: ByteArray?)

  /**
   * Bind [long] to the underlying statement at [index].
   */
  fun bindLong(index: Int, long: Long?)

  /**
   * Bind [double] to the underlying statement at [index].
   */
  fun bindDouble(index: Int, double: Double?)

  /**
   * Bind [string] to the underlying statement at [index].
   */
  fun bindString(index: Int, string: String?)
}
