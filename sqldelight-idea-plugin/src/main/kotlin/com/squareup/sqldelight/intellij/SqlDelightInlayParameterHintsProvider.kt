package com.squareup.sqldelight.intellij

import com.alecstrong.sql.psi.core.psi.SqlInsertStmt
import com.alecstrong.sql.psi.core.psi.SqlInsertStmtValues
import com.alecstrong.sql.psi.core.psi.SqlValuesExpression
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.refactoring.suggested.startOffset

class SqlDelightInlayParameterHintsProvider : InlayParameterHintsProvider {

  override fun getParameterHints(element: PsiElement): List<InlayInfo> {
    if (element !is SqlValuesExpression || element.parent !is SqlInsertStmtValues) {
      return emptyList()
    }
    return element.parentOfType<SqlInsertStmt>()?.columnNameList.orEmpty()
      .zip(element.exprList) { col, expr ->
        InlayInfo(col.name, expr.startOffset)
      }
  }

  override fun getDefaultBlackList(): Set<String> {
    return emptySet()
  }
}
