package com.alecstrong.sqlite.android.util

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil

internal inline fun <reified R : PsiElement> PsiElement.parentOfType(): R? =
    PsiTreeUtil.getParentOfType(this, R::class.java)

internal inline fun <reified R : PsiElement> PsiElement.prevSiblingOfType(): R? =
    PsiTreeUtil.getPrevSiblingOfType(this, R::class.java)

internal inline fun <reified R : PsiElement> PsiElement.childOfType(): R? =
    PsiTreeUtil.getChildOfType(this, R::class.java)

internal fun PsiElement.findFirstParent(condition: (element: PsiElement) -> Boolean) =
    PsiTreeUtil.findFirstParent(this, condition)

internal fun PsiElement.getDeepestFirst() = PsiTreeUtil.getDeepestFirst(this)

internal fun PsiFile.collectElements(predicate: (element: PsiElement) -> Boolean) =
    PsiTreeUtil.collectElements(this, predicate)

internal fun PsiFile.processElements(processor: (element: PsiElement) -> Boolean) =
    PsiTreeUtil.processElements(this, processor)

internal val PsiElement.elementType: IElementType
    get() = node.elementType