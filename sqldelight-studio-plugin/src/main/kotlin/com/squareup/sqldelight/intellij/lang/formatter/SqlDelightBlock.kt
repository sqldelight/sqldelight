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
package com.squareup.sqldelight.intellij.lang.formatter

import com.intellij.formatting.Alignment
import com.intellij.formatting.Block
import com.intellij.formatting.Indent
import com.intellij.formatting.Spacing
import com.intellij.formatting.Wrap
import com.intellij.formatting.WrapType
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.formatter.common.AbstractBlock
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.intellij.lang.SqliteTokenTypes

internal class SqlDelightBlock(
    node: ASTNode, wrap: Wrap?, alignment: Alignment?
) : AbstractBlock(node, wrap, alignment) {
  private val children by lazy {
    if (isLeaf) emptyList<Block>()
    else node.getChildren(null)
        .filterNot { it is PsiWhiteSpace }
        .filter { it.textRange.startOffset < it.textRange.endOffset }
        .filter { it.text != ";" }
        .map { it.asBlock() }
  }

  override fun buildChildren() = children
  override fun getIndent() = Indent.getNoneIndent()
  override fun isLeaf() =
      node.elementType == SqliteTokenTypes.RULE_ELEMENT_TYPES[SqliteParser.RULE_import_stmt]

  override fun getSpacing(c1: Block?, c2: Block): Spacing {
    if (c1 == null) return Spacing.createSpacing(0, 0, 0, false, 0)
    else if (c2 is SqlDelightBlock && c2.isLeaf) {
      return Spacing.createSpacing(0, 0, 1, false, 0)
    }
    return Spacing.createSpacing(0, 0, 2, false, 0)
  }

  fun ASTNode.asBlock() = when (elementType) {
    SqliteTokenTypes.RULE_ELEMENT_TYPES[SqliteParser.RULE_sql_stmt] -> SqlStmtBlock(this)
    SqliteTokenTypes.RULE_ELEMENT_TYPES[SqliteParser.RULE_create_table_stmt] -> CreateTableBlock(this)
    else -> SqlDelightBlock(this, Wrap.createWrap(WrapType.NONE, false), Alignment.createAlignment())
  }
}