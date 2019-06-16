package com.squareup.sqldelight.intellij

import com.alecstrong.sqlite.psi.core.psi.NamedElement
import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement

class SqlDelightRefactoringSupportProvider : RefactoringSupportProvider() {
  override fun isInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean {
    return element is NamedElement
  }
}