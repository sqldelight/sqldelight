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
import com.squareup.sqldelight.intellij.lang.formatter.util.addIfValid
import java.util.ArrayList

internal class JavadocBlock(private val node: ASTNode) : Block {
  private val children by lazy {
    node.javadocChildren()
  }

  override fun getAlignment() = Alignment.createAlignment()
  override fun isIncomplete() = !node.text.endsWith("*/")
  override fun isLeaf() = false
  override fun getTextRange() = node.textRange
  override fun getChildAttributes(newChildIndex: Int) = ChildAttributes(null, null)
  override fun getWrap() = Wrap.createWrap(WrapType.NONE, false)
  override fun getIndent() = Indent.getNoneIndent()
  override fun getSubBlocks() = children
  override fun getSpacing(child1: Block?, child2: Block) =
    if (child1 == null) Spacing.createSpacing(0, 0, 0, false, 0)
    else Spacing.createSpacing(0, 0, 1, false, 0)

  private fun ASTNode.javadocChildren(): List<Block> {
    if (text.lines().size == 1) return listOf(AbstractLeaf(textRange))
    val result = ArrayList<Block>()
    val withoutClose = text.substringBeforeLast("*/").trim(' ', '\n')
    var start = startOffset
    withoutClose.toCharArray().forEachIndexed { index, c ->
      when (c) {
        '\n' -> {
          if (result.isEmpty()) {
            result.addIfValid(AbstractLeaf(TextRange(start, startOffset + index)))
          } else {
            result.addIfValid(AbstractLeaf(
                TextRange(start, startOffset + index), myIndent = Indent.getSpaceIndent(1)
            ))
          }
          start = startOffset + index + 1
        }
        ' ' -> {
          if (start == startOffset + index) start++ // Remove spaces from the start of a line
        }
      }
    }
    if (start < startOffset + withoutClose.length) {
      result.addIfValid(AbstractLeaf(
          TextRange(start, startOffset + withoutClose.length),
          myIndent = Indent.getSpaceIndent(1)
      ))
    }
    // Add the javadoc closer (*/)
    result.addIfValid(AbstractLeaf(
        TextRange(startOffset + textLength - 2, startOffset + textLength),
        myIndent = Indent.getSpaceIndent(1)
    ))
    return result
  }
}
