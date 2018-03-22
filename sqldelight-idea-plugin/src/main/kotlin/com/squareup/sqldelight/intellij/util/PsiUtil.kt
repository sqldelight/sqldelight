package com.squareup.sqldelight.intellij.util

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

internal inline fun <reified T : PsiElement> PsiElement.childOfType(): T? =
    PsiTreeUtil.getChildOfType(this, T::class.java)

internal inline fun <reified T : PsiElement> PsiElement.prevSiblingOfType(): PsiElement? =
    PsiTreeUtil.getPrevSiblingOfType(this, T::class.java)
