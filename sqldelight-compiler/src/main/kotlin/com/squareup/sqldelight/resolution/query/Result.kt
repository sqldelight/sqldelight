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
import java.util.ArrayList
import java.util.Stack

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
  fun merge(other: Result): Result

  /**
   * Expands this result to all the result columns it returns.
   */
  fun expand(): List<Value>

  fun annotations(): List<AnnotationSpec> {
    if (!nullable && javaType.isPrimitive) return emptyList()
    if (nullable) return listOf(AnnotationSpec.builder(SqliteCompiler.NULLABLE).build())
    else return listOf(AnnotationSpec.builder(SqliteCompiler.NON_NULL).build())
  }
}

/**
 * Merges a list of results into this one, modifying type and nullability if needed
 * to fit other's results. Results may also be expanded if necessary (for example merging
 * a table of 4 values against 4 values will expand to 4 values).
 */
internal fun List<Result>.merge(other: List<Result>): List<Result> {
  val pushStack: (Result, Stack<Result>) -> Stack<Result> = { result, stack ->
    stack.push(result)
    stack
  }
  // The top of these stacks represent the current result column we are trying to add.
  val resultStack = foldRight(Stack<Result>(), pushStack)
  val otherResultStack = other.foldRight(Stack<Result>(), pushStack)
  val results = ArrayList<Result>()
  while (!resultStack.empty() && !otherResultStack.empty()) {
    val result = resultStack.pop()
    val otherResult = otherResultStack.pop()
    if (result is Table && (otherResult !is Table || result.table != otherResult.table)) {
      // Incompatible tables or other result is not a table, unpack the result and try again.
      otherResultStack.push(otherResult)
      result.expand().foldRight(resultStack, pushStack)
      continue
    } else if (result is QueryResults &&
        (otherResult !is QueryResults || result.originalViewName != otherResult.originalViewName)) {
      // Incompatible views or other result is not a view, unpack the result and try again.
      otherResultStack.push(otherResult)
      result.expand().foldRight(resultStack, pushStack)
      continue
    } else if ((result !is Table && otherResult is Table) ||
        (result !is QueryResults && otherResult is QueryResults)) {
      // If we got this far it means the table or view we are trying to merge against does not
      // match the current result, so unpack it and try again.
      resultStack.push(result)
      otherResult.expand().foldRight(otherResultStack, pushStack)
      continue
    }
    // Compatible types. Perform merge.
    results.add(result.merge(otherResult))
  }
  return results
}

/**
 * Returns the total number of result columns for a list of results.
 */
internal fun List<Result>.resultColumnSize() = fold(0, { size, result -> size + result.size() })

/**
 * Returns a value expression which can contain any of the given results.
 */
internal fun List<Result>.ceilType(): SqliteType {
  // Start with the lowest bound and move up.
  val allTypes = filterIsInstance<Value>().map { it.dataType }.toSet()
  if (allTypes.contains(SqliteType.BLOB)) {
    return SqliteType.BLOB
  } else if (allTypes.contains(SqliteType.TEXT)) {
    return SqliteType.TEXT
  } else if (allTypes.contains(SqliteType.REAL)) {
    return SqliteType.REAL
  } else {
    return SqliteType.INTEGER
  }
}

internal fun List<Result>.ceilValue(expression: SqliteParser.ExprContext) =
    Value(expression, ceilType(), any { it.nullable })
