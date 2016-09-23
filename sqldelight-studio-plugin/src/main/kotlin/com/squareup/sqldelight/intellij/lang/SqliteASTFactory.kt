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
package com.squareup.sqldelight.intellij.lang

import com.intellij.lang.ASTFactory
import com.intellij.lang.ASTNode
import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.impl.source.tree.FileElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.squareup.sqldelight.SqliteLexer
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.intellij.lang.SqliteTokenTypes.RULE_ELEMENT_TYPES
import com.squareup.sqldelight.intellij.lang.SqliteTokenTypes.TOKEN_ELEMENT_TYPES
import com.squareup.sqldelight.intellij.psi.ASTWrapperPsiElement
import com.squareup.sqldelight.intellij.psi.ClassNameElement
import com.squareup.sqldelight.intellij.psi.IdentifierElement
import com.squareup.sqldelight.intellij.psi.ImportElement
import com.squareup.sqldelight.intellij.psi.ParseElement
import com.squareup.sqldelight.intellij.psi.SqlDelightComment
import com.squareup.sqldelight.intellij.psi.SqliteElement.ColumnAliasElement
import com.squareup.sqldelight.intellij.psi.SqliteElement.ColumnNameElement
import com.squareup.sqldelight.intellij.psi.SqliteElement.SqlStmtNameElement
import com.squareup.sqldelight.intellij.psi.SqliteElement.TableAliasElement
import com.squareup.sqldelight.intellij.psi.SqliteElement.TableNameElement
import com.squareup.sqldelight.intellij.psi.SqliteElement.ViewNameElement

class SqliteASTFactory : ASTFactory() {
  override fun createComposite(type: IElementType) =
      when (type) {
        is IFileElementType -> FileElement(type, null)
        else -> CompositeElement(type)
      }

  override fun createLeaf(type: IElementType, text: CharSequence) =
      when (type) {
        TOKEN_ELEMENT_TYPES[SqliteLexer.IDENTIFIER] -> IdentifierElement(type, text)
        TOKEN_ELEMENT_TYPES[SqliteLexer.JAVADOC_COMMENT] -> SqlDelightComment(type, text)
        TOKEN_ELEMENT_TYPES[SqliteLexer.MULTILINE_COMMENT] -> SqlDelightComment(type, text)
        else -> LeafPsiElement(type, text)
      }
}

private val factories = mapOf(
    RULE_ELEMENT_TYPES[SqliteParser.RULE_parse] to ::ParseElement,
    RULE_ELEMENT_TYPES[SqliteParser.RULE_table_name] to ::TableNameElement,
    RULE_ELEMENT_TYPES[SqliteParser.RULE_column_name] to ::ColumnNameElement,
    RULE_ELEMENT_TYPES[SqliteParser.RULE_sql_stmt_name] to ::SqlStmtNameElement,
    RULE_ELEMENT_TYPES[SqliteParser.RULE_view_name] to ::ViewNameElement,
    RULE_ELEMENT_TYPES[SqliteParser.RULE_table_alias] to ::TableAliasElement,
    RULE_ELEMENT_TYPES[SqliteParser.RULE_column_alias] to ::ColumnAliasElement,
    RULE_ELEMENT_TYPES[SqliteParser.RULE_import_stmt] to ::ImportElement,
    RULE_ELEMENT_TYPES[SqliteParser.RULE_java_type] to ::ClassNameElement
)

internal fun ASTNode.asPSINode() = factories[elementType]?.call(this) ?: ASTWrapperPsiElement(this)

