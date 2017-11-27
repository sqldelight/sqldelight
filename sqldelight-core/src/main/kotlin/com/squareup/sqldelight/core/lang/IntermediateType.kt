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
package com.squareup.sqldelight.core.lang

import com.alecstrong.sqlite.psi.core.psi.SqliteColumnDef
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

/**
 * Internal representation for a column type, which has SQLite data affinity as well as JVM class
 * type.
 */
internal data class IntermediateType(
    val sqliteType: SqliteType,
    val javaType: TypeName = sqliteType.javaType,
    /**
     * The column definition this type is sourced from, or null if there is none.
     */
    val column: SqliteColumnDef? = null,
    /**
     * The name of this intermediate type as exposed in the generated api.
     */
    val name: String = "value"
) {
  fun asNullable() = copy(javaType = javaType.asNullable())

  fun asNonNullable() = copy(javaType = javaType.asNonNullable())

  fun nullableIf(predicate: Boolean): IntermediateType {
    return if (predicate) asNullable() else asNonNullable()
  }

  enum class SqliteType(val javaType: TypeName) {
    ARGUMENT(ANY),
    NULL(Nothing::class.asClassName().asNullable()),
    INTEGER(LONG),
    REAL(FLOAT),
    TEXT(String::class.asTypeName()),
    BLOB(ByteArray::class.asTypeName());
  }
}
