/*
 * Copyright (C) 2016 Square, Inc.
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
package com.squareup.sqldelight

/** A [ColumnAdapter] which maps the enum class `T` to a string in the database. */
class EnumColumnAdapter<T : Enum<T>> @PublishedApi internal constructor(
  private val enumValues: Array<out T>
) : ColumnAdapter<T, String> {
  override fun decode(databaseValue: String) = enumValues.first { it.name == databaseValue }

  override fun encode(value: T) = value.name
}

@Suppress("FunctionName") // Emulating a constructor.
inline fun <reified T : Enum<T>> EnumColumnAdapter(): EnumColumnAdapter<T> {
  return EnumColumnAdapter(enumValues())
}
