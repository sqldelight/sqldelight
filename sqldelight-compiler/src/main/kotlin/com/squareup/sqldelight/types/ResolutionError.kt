package com.squareup.sqldelight.types

import com.squareup.sqldelight.SqliteParser
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
}