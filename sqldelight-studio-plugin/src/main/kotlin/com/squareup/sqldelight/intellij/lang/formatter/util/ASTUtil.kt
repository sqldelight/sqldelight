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
package com.squareup.sqldelight.intellij.lang.formatter.util

import com.intellij.formatting.Block
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiWhiteSpace
import java.util.ArrayList

internal fun ASTNode.siblingSemicolon(): Int? =
  if (text == ";") textRange.endOffset
  else if (treeNext == null) null
  else treeNext.siblingSemicolon()

internal fun List<ASTNode>.textRange() = dropWhile { it is PsiWhiteSpace }
    .dropLastWhile { it is PsiWhiteSpace }
    .fold(null as TextRange?, { textRange, node ->
      if (textRange == null) {
        node.textRange
      } else {
        TextRange(
            Math.min(textRange.startOffset, node.startOffset),
            Math.max(textRange.endOffset, node.textRange.endOffset)
        )
      }
    })

internal fun ASTNode.rangeToEnd(): TextRange =
    TextRange(startOffset, treeNext?.rangeToEnd()?.endOffset ?: textRange.endOffset)

internal fun ArrayList<Block>.addIfValid(block: Block) {
  if (block.textRange.startOffset >= block.textRange.endOffset) return
  add(block)
}
