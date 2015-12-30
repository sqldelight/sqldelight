package com.alecstrong.sqlite.android.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import java.util.ArrayList

open class ASTWrapperPsiElement(node: ASTNode) : com.intellij.extapi.psi.ASTWrapperPsiElement(
    node) {
  override fun getChildren(): Array<PsiElement> {
    var psiChild: PsiElement? = firstChild ?: return PsiElement.EMPTY_ARRAY

    val result = ArrayList<PsiElement>()
    while (psiChild != null) {
      result.add(psiChild)
      psiChild = psiChild.nextSibling
    }
    return PsiUtilCore.toPsiElementArray(result)
  }
}
