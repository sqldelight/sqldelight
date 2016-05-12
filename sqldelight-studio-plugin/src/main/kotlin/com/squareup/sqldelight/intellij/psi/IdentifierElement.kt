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
package com.squareup.sqldelight.intellij.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.intellij.lang.SqliteTokenTypes
import com.squareup.sqldelight.intellij.util.SqlitePsiUtils

class IdentifierElement(type: IElementType, text: CharSequence) : LeafPsiElement(type,
    text), PsiNamedElement {
  private var hardcodedName: String? = null

  override fun getName() = hardcodedName ?: text

  override fun setName(name: String): PsiElement {
    replace(SqlitePsiUtils.createLeafFromText(project, context, name,
        SqliteTokenTypes.TOKEN_ELEMENT_TYPES[SqliteParser.IDENTIFIER]))
    hardcodedName = name
    return this
  }

  override fun getReference() = SqlDelightElementRef(this, text)
  override fun toString() = "${javaClass.simpleName}(${elementType.toString()})"
}
