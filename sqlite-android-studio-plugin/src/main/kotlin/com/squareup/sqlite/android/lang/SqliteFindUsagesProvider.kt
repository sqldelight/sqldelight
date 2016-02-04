package com.squareup.sqlite.android.lang

import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.squareup.sqlite.android.psi.SqliteElement.ColumnNameElement
import com.squareup.sqlite.android.psi.SqliteElement.SqlStmtNameElement

class SqliteFindUsagesProvider : FindUsagesProvider {
  override fun getWordsScanner() = null
  override fun canFindUsagesFor(psiElement: PsiElement) = psiElement is ColumnNameElement
      || psiElement is SqlStmtNameElement

  override fun getHelpId(psiElement: PsiElement) = null
  override fun getDescriptiveName(element: PsiElement) = element.text
  override fun getNodeText(element: PsiElement, useFullName: Boolean) = element.parent.text
  override fun getType(element: PsiElement) =
      when (element) {
        is ColumnNameElement -> "sqlite column"
        is SqlStmtNameElement -> "sqlite statement"
        else -> ""
      }
}
