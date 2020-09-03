package com.squareup.sqldelight.core.lang

import com.alecstrong.sql.psi.core.DialectPreset
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.openapi.project.Project
import com.squareup.sqldelight.core.SqlDelightProjectService
import com.squareup.sqldelight.core.SqldelightParserUtil
import com.squareup.sqldelight.core.lang.psi.FunctionExprMixin

internal class ParserUtil {
  private var dialect: DialectPreset? = null

  fun initializeDialect(project: Project) {
    val newDialect = SqlDelightProjectService.getInstance(project).dialectPreset
    if (newDialect != dialect) {
      newDialect.setup()
      SqldelightParserUtil.overrideSqlParser()
      dialect = newDialect

      val currentElementCreation = SqldelightParserUtil.createElement
      SqldelightParserUtil.createElement = {
        when (it.elementType) {
          SqlTypes.FUNCTION_EXPR -> FunctionExprMixin(it)
          else -> currentElementCreation(it)
        }
      }
    }
  }
}
