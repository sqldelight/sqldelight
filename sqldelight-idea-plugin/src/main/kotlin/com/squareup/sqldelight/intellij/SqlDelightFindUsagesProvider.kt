package com.squareup.sqldelight.intellij

import com.alecstrong.sqlite.psi.core.psi.SqliteColumnAlias
import com.alecstrong.sqlite.psi.core.psi.SqliteColumnName
import com.alecstrong.sqlite.psi.core.psi.SqliteCteTableName
import com.alecstrong.sqlite.psi.core.psi.SqliteTableAlias
import com.alecstrong.sqlite.psi.core.psi.SqliteTableName
import com.alecstrong.sqlite.psi.core.psi.SqliteViewName
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.squareup.sqldelight.core.lang.psi.StmtIdentifierMixin

class SqlDelightFindUsagesProvider : FindUsagesProvider {
  override fun getWordsScanner() = null
  override fun getNodeText(element: PsiElement, useFullName: Boolean) = element.text
  override fun getDescriptiveName(element: PsiElement) = element.text
  override fun getHelpId(psiElement: PsiElement) = null

  override fun getType(element: PsiElement): String {
    return when (element) {
      is StmtIdentifierMixin -> "query"
      is SqliteTableName -> "table"
      is SqliteColumnName -> "column"
      is SqliteTableAlias -> "table alias"
      is SqliteColumnAlias -> "column alias"
      is SqliteCteTableName -> "common table"
      is SqliteViewName -> "view"
      else -> throw AssertionError()
    }
  }

  override fun canFindUsagesFor(element: PsiElement): Boolean {
    return when (element) {
      is StmtIdentifierMixin, is SqliteTableName, is SqliteColumnName, is SqliteTableAlias,
      is SqliteColumnAlias, is SqliteViewName, is SqliteCteTableName -> true
      else -> false
    }
  }
}