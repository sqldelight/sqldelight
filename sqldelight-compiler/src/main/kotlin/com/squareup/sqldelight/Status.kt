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

import com.squareup.javapoet.TypeSpec
import com.squareup.sqldelight.resolution.ResolutionError
import com.squareup.sqldelight.resolution.query.QueryResults
import org.antlr.v4.runtime.ParserRuleContext

sealed class Status(val originatingElement: ParserRuleContext) {
  class Success(element: ParserRuleContext, val model: TypeSpec) : Status(element)
  class Failure(element: ParserRuleContext, val errorMessage: String) : Status(element)
  sealed class ValidationStatus(element: ParserRuleContext, val dependencies: Collection<Any>) : Status(element) {
    class Validated(
        element: ParserRuleContext,
        dependencies: Collection<Any>,
        val queries: List<QueryResults>,
        val views: List<QueryResults>
    ) : ValidationStatus(element, dependencies)
    class Invalid(
        val errors: Collection<ResolutionError>,
        dependencies: Collection<Any>
    ) : ValidationStatus(errors.first().originatingElement, dependencies)
  }
}
