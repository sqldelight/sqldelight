/*
 * Copyright (C) 2016 Square, Inc.
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
package com.squareup.sqlite.android.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType
import com.squareup.sqlite.android.SQLiteParser
import com.squareup.sqlite.android.lang.SqliteTokenTypes
import com.squareup.sqlite.android.psi.SqliteElement.ColumnNameElement
import com.squareup.sqlite.android.psi.SqliteElement.SqlStmtNameElement
import com.squareup.sqlite.android.psi.SqliteElement.TableNameElement
import com.squareup.sqlite.android.util.SqlitePsiUtils
import com.squareup.sqlite.android.util.parentOfType

class IdentifierElement(type: IElementType, text: CharSequence) : LeafPsiElement(type,
    text), PsiNamedElement {
  private val ruleRefType = SqliteTokenTypes.TOKEN_ELEMENT_TYPES[SQLiteParser.IDENTIFIER]

  private var hardcodedName: String? = null

  override fun getName() = hardcodedName ?: text

  override fun setName(name: String): PsiElement {
    replace(SqlitePsiUtils.createLeafFromText(project, context, name, ruleRefType))
    hardcodedName = name
    return this
  }

  override fun getReference(): PsiReference? =
      when {
        parentOfType<TableNameElement>() != null -> TableNameElementRef(this, text)
        parentOfType<ColumnNameElement>() != null -> ColumnNameElementRef(this, text)
        parentOfType<SqlStmtNameElement>() != null -> SqlStmtNameElementRef(this, text)
        else -> null
      }

  override fun toString() = "${javaClass.simpleName}(${elementType.toString()})"
}
