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

import com.intellij.lang.CodeDocumentationAwareCommenter
import com.intellij.psi.PsiComment
import com.squareup.sqldelight.SqliteLexer

class SqlDelightCommenter : CodeDocumentationAwareCommenter {
  override fun getLineCommentPrefix() = "--"
  override fun getCommentedBlockCommentPrefix() = "/*"
  override fun getCommentedBlockCommentSuffix() = "*/"
  override fun getBlockCommentPrefix() = null
  override fun getBlockCommentSuffix() = null
  override fun isDocumentationComment(p0: PsiComment?) = false
  override fun getDocumentationCommentTokenType() = null
  override fun getLineCommentTokenType() = SqliteTokenTypes.TOKEN_ELEMENT_TYPES[SqliteLexer.SINGLE_LINE_COMMENT]
  override fun getBlockCommentTokenType() = SqliteTokenTypes.TOKEN_ELEMENT_TYPES[SqliteLexer.MULTILINE_COMMENT]
  override fun getDocumentationCommentLinePrefix() = null
  override fun getDocumentationCommentPrefix() = null
  override fun getDocumentationCommentSuffix() = null
}
