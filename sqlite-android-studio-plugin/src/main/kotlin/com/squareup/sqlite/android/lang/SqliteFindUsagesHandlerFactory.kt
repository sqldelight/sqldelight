package com.squareup.sqlite.android.lang

import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.find.findUsages.JavaFindUsagesHandler
import com.intellij.find.findUsages.JavaFindUsagesHandlerFactory
import com.intellij.psi.PsiElement
import com.squareup.sqlite.android.psi.SqliteElement.ColumnNameElement
import com.squareup.sqlite.android.psi.SqliteElement.SqlStmtNameElement
import com.squareup.sqlite.android.util.getSecondaryElements

class SqliteFindUsagesHandlerFactory : FindUsagesHandlerFactory() {
  override fun canFindUsages(element: PsiElement) = element is ColumnNameElement
      || element is SqlStmtNameElement

  override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean) =
      when (element) {
        is ColumnNameElement -> JavaFindUsagesHandler(element, element.getSecondaryElements(),
            JavaFindUsagesHandlerFactory.getInstance(element.getProject()))
        is SqlStmtNameElement -> JavaFindUsagesHandler(element, element.getSecondaryElements(),
            JavaFindUsagesHandlerFactory.getInstance(element.getProject()))
        else -> null
      }
}
