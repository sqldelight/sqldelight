package com.squareup.sqldelight.intellij

import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.SqlInsertStmtValues
import com.alecstrong.sql.psi.core.psi.SqlValuesExpression
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class SqlDelightInlayParameterHintsProvider : InlayParameterHintsProvider {

  override fun getParameterHints(element: PsiElement): List<InlayInfo> {
    if (element !is SqlValuesExpression || element.parent !is SqlInsertStmtValues) {
      return emptyList()
    }
    return runCatching { element.queryAvailable(element) }
      .getOrDefault(emptyList())
      .flatMap(QueryElement.QueryResult::columns)
      .zip(element.exprList) { col, expr ->
        InlayInfo(col.element.text, expr.startOffset)
      }
  }

  override fun getDefaultBlackList(): Set<String> {
    return emptySet()
  }
}
