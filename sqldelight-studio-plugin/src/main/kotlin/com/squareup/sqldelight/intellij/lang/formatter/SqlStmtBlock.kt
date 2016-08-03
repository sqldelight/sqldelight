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
import com.intellij.formatting.ChildAttributes
import com.intellij.formatting.Indent
import com.intellij.formatting.Spacing
import com.intellij.formatting.Wrap
import com.intellij.formatting.WrapType
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiWhiteSpace
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.intellij.lang.SqliteTokenTypes
import com.squareup.sqldelight.intellij.lang.formatter.util.rangeToEnd
import com.squareup.sqldelight.intellij.lang.formatter.util.siblingSemicolon
import java.util.ArrayList

internal class SqlStmtBlock(private val node: ASTNode) : Block {
  private val children by lazy {
    var afterColon = false
    val result = ArrayList<Block>()
    for (child in node.getChildren(null)) {
      if (child is PsiWhiteSpace) continue
      if (child.textRange.startOffset >= child.textRange.endOffset) continue
      if (child.elementType == SqliteTokenTypes.TOKEN_ELEMENT_TYPES[SqliteParser.JAVADOC_COMMENT]) {
        result.add(JavadocBlock(child))
      } else if (child.elementType == SqliteTokenTypes.RULE_ELEMENT_TYPES[SqliteParser.RULE_sql_stmt_name]) {
        result.add(AbstractLeaf(child.textRange, lineBreaks = 1))
      } else if (child.text == ":") {
        afterColon = true
        result.add(AbstractLeaf(child.textRange))
      } else if (afterColon) {
        // The rest of the sql statement is a single node with a new line.
        result.add(AbstractLeaf(child.rangeToEnd(), lineBreaks = 1))
        break
      } else {
        result.add(AbstractLeaf(child.textRange))
      }
    }
    result.add(AbstractLeaf(TextRange(result.last().textRange.endOffset, textRange.endOffset)))
    result
  }

  override fun getAlignment() = Alignment.createAlignment()
  override fun getIndent() = Indent.getNoneIndent()
  override fun getTextRange() = TextRange(
      node.textRange.startOffset,
      node.siblingSemicolon() ?: node.textRange.endOffset
  )
  override fun isIncomplete() = node.siblingSemicolon() == null
  override fun isLeaf() = false
  override fun getChildAttributes(newChildIndex: Int) = ChildAttributes(null, null)
  override fun getWrap() = Wrap.createWrap(WrapType.NONE, false)
  override fun getSubBlocks() = children
  override fun getSpacing(child1: Block?, child2: Block): Spacing {
    if (child1 == null) return Spacing.createSpacing(0, 0, 0, false, 0)
    if (child2 is AbstractLeaf) return Spacing.createSpacing(0, 0, child2.lineBreaks, false, 0)
    return Spacing.createSpacing(0, 0, 1, false, 0)
  }
}
