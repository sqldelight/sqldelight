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
package com.squareup.sqldelight.resolution.query

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.TypeName
import com.squareup.sqldelight.SqliteCompiler
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.types.SqliteType
import org.antlr.v4.runtime.ParserRuleContext

/**
 * Will get represented as a single method on the generated query interface and is acquired
 * from a SQL select during resolution.
 */
interface Result {
  /**
   * The name to be used for the generated interface method.
   */
  val name: String
  /**
   * The origin rule of this element.
   */
  val element: ParserRuleContext
  /**
   * The type to be used for the generated interface method.
   */
  val javaType: TypeName
  /**
   * Whether the generated interface method is nullable or not.
   */
  val nullable: Boolean
  /**
   * The total number of columns this result contains.
   */
  fun size(): Int
  /**
   * Finds the given element in this result, or null if it could not be found.
   */
  fun findElement(columnName: String, tableName: String? = null): List<Value>
  fun columnNames(): List<String>
  fun tableNames(): List<String>

  fun annotations(): List<AnnotationSpec> {
    if (!nullable && javaType.isPrimitive) return emptyList()
    if (nullable) return listOf(AnnotationSpec.builder(SqliteCompiler.NULLABLE).build())
    else return listOf(AnnotationSpec.builder(SqliteCompiler.NON_NULL).build())
  }
}

/**
 * Returns the total number of result columns for a list of results.
 */
internal fun List<Result>.resultColumnSize() = fold(0, { size, result -> size + result.size() })

/**
 * Returns a value expression which can contain any of the given results.
 */
internal fun List<Result>.ceilValue(expression: SqliteParser.ExprContext): Value {
  // Start with the lowest bound and move up.
  val allTypes = filterIsInstance<Value>().map { it.javaType }.toSet()
  if (allTypes.contains(SqliteType.BLOB.defaultType)) {
    return Value(expression, SqliteType.BLOB.defaultType, any { it.nullable })
  } else if (allTypes.contains(SqliteType.TEXT.defaultType)) {
    return Value(expression, SqliteType.TEXT.defaultType, any { it.nullable })
  } else if (allTypes.contains(SqliteType.REAL.defaultType)) {
    return Value(expression, SqliteType.REAL.defaultType, any { it.nullable })
  } else {
    return Value(expression, SqliteType.INTEGER.defaultType, any { it.nullable })
  }
}
