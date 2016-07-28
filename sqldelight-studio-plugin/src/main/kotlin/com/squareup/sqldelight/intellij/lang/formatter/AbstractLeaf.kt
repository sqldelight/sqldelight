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
import com.intellij.openapi.util.TextRange

internal class AbstractLeaf(
    private val range: TextRange,
    val lineBreaks: Int = 0,
    private val myIndent: Indent = Indent.getNoneIndent()
): Block {
  override fun getAlignment() = Alignment.createAlignment()
  override fun isIncomplete() = false
  override fun isLeaf() = true
  override fun getSpacing(chid1: Block?, child2: Block) = null
  override fun getSubBlocks() = emptyList<Block>()
  override fun getChildAttributes(newChildIndex: Int) = ChildAttributes(null, null)
  override fun getWrap() = null
  override fun getIndent() = myIndent
  override fun getTextRange() = range
}
