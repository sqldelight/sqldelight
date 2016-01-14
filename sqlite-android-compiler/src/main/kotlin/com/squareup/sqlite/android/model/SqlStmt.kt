package com.squareup.sqlite.android.model

import com.google.common.base.CaseFormat.LOWER_CAMEL
import com.google.common.base.CaseFormat.UPPER_UNDERSCORE

class SqlStmt<T>(identifier: String, stmt: String, startOffset: Int, allReplacements: List<SqlStmt.Replacement>,
    originatingElement: T) : SqlElement<T>(originatingElement) {

  val stmt: String
  val identifier = identifier
    get() = fieldName(field)

  init {
    var nextOffset = 0
    this.stmt = allReplacements
        .filter({ it.startOffset > startOffset && it.endOffset < startOffset + stmt.length })
        .fold(StringBuilder(), { builder, replacement ->
          builder.append(stmt.substring(nextOffset, replacement.startOffset - startOffset)).append(
              replacement.replacementText)
          nextOffset = replacement.endOffset - startOffset
          builder
        })
        .append(stmt.substring(nextOffset, stmt.length))
        .toString()
  }

  data class Replacement(internal val startOffset: Int, internal val endOffset: Int, internal val replacementText: String)

  companion object {
    fun fieldName(identifier: String) = LOWER_CAMEL.to(UPPER_UNDERSCORE, identifier)
  }
}
