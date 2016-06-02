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
package com.squareup.sqldelight.resolution

import com.squareup.sqldelight.types.Value
import org.antlr.v4.runtime.ParserRuleContext

internal data class Resolution(
    val values: List<Value> = emptyList(),
    val errors: List<ResolutionError> = emptyList()
) {
  constructor(error: ResolutionError) : this(emptyList(), listOf(error))

  operator fun plus(other: Resolution) = Resolution(values + other.values, errors + other.errors)

  operator fun plus(error: ResolutionError) = plus(Resolution(error))

  fun findElement(element: ParserRuleContext?, source: ParserRuleContext?, elementToFind: Int?) =
      if (element != null && element.start.startIndex == elementToFind) {
        this + Resolution(ResolutionError.ElementFound(source!!))
      } else {
        this
      }
}