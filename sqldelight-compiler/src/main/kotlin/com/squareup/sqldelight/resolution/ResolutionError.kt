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

import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.types.Value
import org.antlr.v4.runtime.ParserRuleContext

sealed class ResolutionError(val originatingElement: ParserRuleContext, val errorMessage: String) {
  class CompoundError(
      originatingElement: SqliteParser.Select_or_valuesContext,
      errorMessage: String
  ) : ResolutionError(originatingElement, errorMessage)
  class ValuesError(
      originatingElement: SqliteParser.ValuesContext,
      errorMessage: String
  ) : ResolutionError(originatingElement, errorMessage)
  class ColumnNameNotFound(
      originatingElement: ParserRuleContext,
      errorMessage: String,
      val availableColumns: Collection<Value>
  ) : ResolutionError(originatingElement, errorMessage)
  class ColumnOrTableNameNotFound(
      originatingElement: ParserRuleContext,
      errorMessage: String,
      val availableColumns: Collection<Value>,
      val tableName: String?
  ) : ResolutionError(originatingElement, errorMessage)
  class ExpressionError(
      originatingElement: ParserRuleContext,
      errorMessage: String
  ) : ResolutionError(originatingElement, errorMessage)
  class RecursiveResolution(
      originatingElement: ParserRuleContext,
      errorMessage: String
  ) : ResolutionError(originatingElement, errorMessage)
  class CreateTableError(
      originatingElement: ParserRuleContext,
      errorMessage: String
  ) : ResolutionError(originatingElement, errorMessage)
  class InsertError(
      originatingElement: ParserRuleContext,
      errorMessage: String
  ) : ResolutionError(originatingElement, errorMessage)
  class CollisionError(
      originatingElement: ParserRuleContext,
      errorMessage: String
  ) : ResolutionError(originatingElement, errorMessage)
  class TableNameNotFound(
      originatingElement: ParserRuleContext,
      errorMessage: String,
      val availableTableNames: Collection<String>
  ) : ResolutionError(originatingElement, errorMessage)
  class WithTableError(
      originatingElement: ParserRuleContext,
      errorMessage: String
  ) : ResolutionError(originatingElement, errorMessage)
  class IncompleteRule(
      originatingElement: ParserRuleContext,
      errorMessage: String
  ) : ResolutionError(originatingElement, errorMessage)
  class ElementFound(
      originatingElement: ParserRuleContext
  ) : ResolutionError(originatingElement, "Element Found")
}