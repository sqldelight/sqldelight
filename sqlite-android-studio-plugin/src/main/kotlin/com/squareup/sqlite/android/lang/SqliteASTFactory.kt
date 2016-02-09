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
package com.squareup.sqlite.android.lang

import com.intellij.lang.ASTFactory
import com.intellij.lang.ASTNode
import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.impl.source.tree.FileElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.squareup.sqlite.android.SqliteLexer
import com.squareup.sqlite.android.SqliteParser
import com.squareup.sqlite.android.lang.SqliteTokenTypes.RULE_ELEMENT_TYPES
import com.squareup.sqlite.android.lang.SqliteTokenTypes.TOKEN_ELEMENT_TYPES
import com.squareup.sqlite.android.psi.ASTWrapperPsiElement
import com.squareup.sqlite.android.psi.ClassNameElement
import com.squareup.sqlite.android.psi.IdentifierElement
import com.squareup.sqlite.android.psi.ParseElement
import com.squareup.sqlite.android.psi.SqliteElement.ColumnNameElement
import com.squareup.sqlite.android.psi.SqliteElement.SqlStmtNameElement
import com.squareup.sqlite.android.psi.SqliteElement.TableNameElement

class SqliteASTFactory : ASTFactory() {
  override fun createComposite(type: IElementType) =
      when (type) {
        is IFileElementType -> FileElement(type, null)
        else -> CompositeElement(type)
      }

  override fun createLeaf(type: IElementType, text: CharSequence) =
      when (type) {
        TOKEN_ELEMENT_TYPES[SqliteLexer.IDENTIFIER] -> IdentifierElement(type, text)
        TOKEN_ELEMENT_TYPES[SqliteLexer.STRING_LITERAL] -> ClassNameElement(type, text)
        else -> LeafPsiElement(type, text)
      }
}

private val factories = mapOf(
    RULE_ELEMENT_TYPES[SqliteParser.RULE_parse] to ::ParseElement,
    RULE_ELEMENT_TYPES[SqliteParser.RULE_table_name] to ::TableNameElement,
    RULE_ELEMENT_TYPES[SqliteParser.RULE_column_name] to ::ColumnNameElement,
    RULE_ELEMENT_TYPES[SqliteParser.RULE_sql_stmt_name] to ::SqlStmtNameElement
)

internal fun ASTNode.asPSINode() = factories[elementType]?.call(this) ?: ASTWrapperPsiElement(this)

