package com.squareup.sqldelight.core.lang.util

import com.alecstrong.sqlite.psi.core.psi.SqliteCompositeElement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.squareup.sqldelight.core.lang.SqlDelightFile

internal inline fun <reified R: PsiElement> PsiElement.parentOfType(): R {
  return PsiTreeUtil.getParentOfType(this, R::class.java)!!
}

internal fun SqliteCompositeElement.sqFile(): SqlDelightFile = containingFile as SqlDelightFile