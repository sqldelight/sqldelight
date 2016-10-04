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
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.intellij.lang.SqliteTokenTypes
import com.squareup.sqldelight.intellij.lang.formatter.util.addIfValid
import com.squareup.sqldelight.intellij.lang.formatter.util.siblingSemicolon
import com.squareup.sqldelight.intellij.lang.formatter.util.textRange
import java.util.ArrayList

internal class CreateTableBlock(private val node: ASTNode): Block {
  private val childAlignment = Alignment.createAlignment()
  private val children by lazy {
    var child = node.firstChildNode
    val result = ArrayList<Block>()
    val unconsumed = ArrayList<ASTNode>()
    while (child != null) {
      if (child.elementType == SqliteTokenTypes.RULE_ELEMENT_TYPES[SqliteParser.RULE_column_def] ||
          child.elementType == SqliteTokenTypes.RULE_ELEMENT_TYPES[SqliteParser.RULE_table_constraint]) {
        // Get rid of trailing newlines from current block and add it.
        val textRange = unconsumed.textRange()
        unconsumed.clear()
        textRange?.let { result.addIfValid(AbstractLeaf(it)) }
        result.addIfValid(AbstractLeaf(child.textRange, lineBreaks = 1, myIndent = Indent.getNormalIndent()))
      } else {
        unconsumed.add(child)
      }
      child = child.treeNext
    }
    val textRange = unconsumed.textRange()
    textRange?.let { result.addIfValid(AbstractLeaf(it, lineBreaks = 1)) }
    result.addIfValid(AbstractLeaf(TextRange(result.last().textRange.endOffset, getTextRange().endOffset)))
    result
  }

  override fun getWrap() = Wrap.createWrap(WrapType.NONE, false)
  override fun getIndent() = Indent.getNoneIndent()
  override fun getAlignment() = Alignment.createAlignment()
  override fun isIncomplete() = node.siblingSemicolon() == null
  override fun getTextRange() = TextRange(
      node.textRange.startOffset,
      node.siblingSemicolon() ?: node.textRange.endOffset
  )
  override fun isLeaf() = false
  override fun getSubBlocks() = children
  override fun getSpacing(child1: Block?, child2: Block) =
    if (child1 == null) Spacing.createSpacing(0, 0, 0, false, 0)
    else if (child2 is AbstractLeaf) Spacing.createSpacing(0, 0, child2.lineBreaks, false, 0)
    else Spacing.createSpacing(0, 0, 1, false, 0)

  override fun getChildAttributes(newChildIndex: Int) =
      ChildAttributes(Indent.getNormalIndent(), childAlignment)
}
