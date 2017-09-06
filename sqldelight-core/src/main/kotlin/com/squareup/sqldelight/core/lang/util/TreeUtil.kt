/*
 * Copyright (C) 2017 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sqldelight.core.lang.util

import com.alecstrong.sqlite.psi.core.psi.AliasElement
import com.alecstrong.sqlite.psi.core.psi.SqliteColumnName
import com.alecstrong.sqlite.psi.core.psi.SqliteExpr
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.squareup.kotlinpoet.TypeName
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.psi.ColumnDefMixin

internal inline fun <reified R: PsiElement> PsiElement.parentOfType(): R {
  return PsiTreeUtil.getParentOfType(this, R::class.java)!!
}

internal fun PsiElement.type(javaType: Boolean): TypeName = when (this) {
  is AliasElement -> source().type(javaType)
  is SqliteColumnName -> (parent as ColumnDefMixin).type()
  is SqliteExpr -> type(javaType)
  else -> throw IllegalStateException("Cannot get function type for psi type ${this.javaClass}")
}

internal fun PsiElement.sqFile(): SqlDelightFile = containingFile as SqlDelightFile
