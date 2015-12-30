package com.alecstrong.sqlite.android.lang

import com.alecstrong.sqlite.android.psi.SqliteElement.ColumnNameElement
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement

class SqliteFindUsagesProvider : FindUsagesProvider {
  override fun getWordsScanner() = null
  override fun canFindUsagesFor(psiElement: PsiElement) = psiElement is ColumnNameElement
  override fun getHelpId(psiElement: PsiElement) = null
  override fun getDescriptiveName(element: PsiElement) = element.text
  override fun getNodeText(element: PsiElement, useFullName: Boolean) = element.parent.text
  override fun getType(element: PsiElement) =
      when (element) {
        is ColumnNameElement -> "sqlite column"
        else -> ""
      }
}
