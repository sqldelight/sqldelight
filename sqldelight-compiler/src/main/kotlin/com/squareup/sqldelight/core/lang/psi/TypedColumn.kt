/*
 * Copyright (C) 2017 Square, Inc.
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
package com.squareup.sqldelight.core.lang.psi

import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.sqldelight.core.lang.IntermediateType
import com.squareup.sqldelight.core.lang.util.type

internal interface TypedColumn {
  /**
   * @return the java type for this column.
   */
  fun type(): IntermediateType

  /**
   * @return the adapter property which will include the type and name of the adapter. If this
   * column does not require an adapter, returns null.
   */
  fun adapter(): PropertySpec?
}

fun SqlColumnName.isColumnSameAs(other: SqlColumnName) = type().let { it.column != null && it.column == other.type().column }
fun SqlColumnName.isTypeSameAs(other: SqlColumnName) = type().asNonNullable().javaType == other.type().asNonNullable().javaType
