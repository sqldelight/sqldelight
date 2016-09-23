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
package com.squareup.sqldelight.types

import com.squareup.javapoet.NameAllocator
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.resolution.query.Value
import java.util.ArrayList
import java.util.LinkedHashSet

/**
 * Represents the type for an argument. All that is contained is a value since the origin of the
 * type may need to be known for adapters.
 */
sealed class ArgumentType(val comparable: Value?) {
  class SingleValue(comparable: Value?) : ArgumentType(comparable)
  class SetOfValues(comparable: Value?) : ArgumentType(comparable)

  override fun toString() = "${this.javaClass.name}: $comparable"

  companion object {
    fun boolean(expression: SqliteParser.ExprContext) =
        SingleValue(Value(expression, SqliteType.INTEGER, false))
  }
}

/**
 * Represents a full argument as it would appear in the generated code.
 */
data class Argument(
    /** The type to be used in the generated signature. */
    val argumentType: ArgumentType,
    /** The text ranges in the original statement this argument replaces */
    val ranges: MutableList<IntRange>,
    /** The index into the signature for this parameter */
    val paramIndex: Int? = null,
    /** The explicit index the bind arg gave (ie ?3) */
    val index: Int? = null,
    /** The explicit name the bind arg gave (:example, @example or $example) */
    val name: String? = null
)

/**
 * All arguments are given a number (even named parameters). Numbering is handed out linearly
 * over the statement. When a question mark or named argument is observed, it is the number
 * after the current highest observed number. If a numbered arg shows up then no computation is
 * needed (other than increasing the highest number seen so far).
 *
 * Additionally, Android is only capable of taking a string array for its args. And if you
 * only specify a single argument in your statement but number it ?100 then the string
 * array will require 99 ignored arguments. Because android only accepts non null strings
 * we will have to do manual string concatenation for other argument types.
 *
 * The argument list stored will be prepared in a way that matches the SQLite rules: Every
 * arg will be numbered and deduped. The receiver list should be already ordered so all that
 * remains is deduping and handing out numbers.
 */
fun List<Argument>.toSqliteArguments(): List<Argument> {
  if (isEmpty()) return this
  val numberedParameters = ArrayList<Argument>()
  val numbersToSkip = LinkedHashSet<Int>()
  val nameAllocator = NameAllocator()
  var highestNumber = 0

  distinctBy { it.ranges }.sortedBy { it.ranges[0].start }.forEachIndexed { i, original ->
    if (numbersToSkip.contains(i)) return@forEachIndexed

    val index = original.index ?: highestNumber + 1
    val name = original.name ?: original.argumentType.comparable?.paramName
    val argument = original.copy(
        name = nameAllocator.newName(if (name == null || name == "expr") "arg$index" else name),
        index = index
    )

    for (j in (i + 1..size - 1) - numbersToSkip) {
      // Dedupe any args with the same index/name.
      if ((get(j).index == argument.index) || (get(j).name != null && get(j).name == argument.name)) {
        argument.ranges.addAll(get(j).ranges)
        numbersToSkip.add(j)
      }
    }
    if (argument.index != null) {
      highestNumber = Integer.max(highestNumber, argument.index)
      numberedParameters.add(argument)
    } else {
      numberedParameters.add(argument.copy(index = ++highestNumber))
    }
  }

  return numberedParameters
}