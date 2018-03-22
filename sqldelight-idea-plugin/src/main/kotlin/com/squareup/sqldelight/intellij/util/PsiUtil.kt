package com.squareup.sqldelight.intellij.util

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil

internal inline fun <reified R : PsiElement> PsiElement.childOfType(): R? =
    PsiTreeUtil.getChildOfType(this, R::class.java)

internal fun ASTNode.childrenWithType(type: IElementType): List<ASTNode> = getChildren(null)
    .filter { it.elementType == type }

internal fun PsiElement.nextLeafOrNull(predicate: PsiElement.() -> Boolean): PsiElement? {
  var next = PsiTreeUtil.nextLeaf(this)
  while (next != null) {
    if (predicate(next)) {
      return next
    }
    next = PsiTreeUtil.nextLeaf(next)
  }
  return null
}

internal inline fun <reified T : PsiElement> PsiElement.prevSiblingOfType(): PsiElement? =
    PsiTreeUtil.getPrevSiblingOfType(this, T::class.java)
