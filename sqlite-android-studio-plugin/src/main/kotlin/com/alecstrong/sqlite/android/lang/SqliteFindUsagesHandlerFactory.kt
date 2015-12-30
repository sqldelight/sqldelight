package com.alecstrong.sqlite.android.lang

import com.alecstrong.sqlite.android.psi.SqliteElement.ColumnNameElement
import com.alecstrong.sqlite.android.util.getSecondaryElements
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.find.findUsages.JavaFindUsagesHandler
import com.intellij.find.findUsages.JavaFindUsagesHandlerFactory
import com.intellij.psi.PsiElement

class SqliteFindUsagesHandlerFactory : FindUsagesHandlerFactory() {
  override fun canFindUsages(element: PsiElement) = element is ColumnNameElement

  override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean) =
      when (element) {
        is ColumnNameElement -> JavaFindUsagesHandler(element, element.getSecondaryElements(),
            JavaFindUsagesHandlerFactory.getInstance(element.getProject()))
        else -> null
      }
}
