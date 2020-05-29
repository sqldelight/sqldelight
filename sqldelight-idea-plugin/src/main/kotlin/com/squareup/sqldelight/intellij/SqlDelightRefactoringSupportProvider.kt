package com.squareup.sqldelight.intellij

import com.alecstrong.sql.psi.core.psi.NamedElement
import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement

class SqlDelightRefactoringSupportProvider : RefactoringSupportProvider() {
  override fun isMemberInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean {
    return element is NamedElement
  }
}
