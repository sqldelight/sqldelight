/*
 * Copyright (C) 2018 Square, Inc.
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

import com.alecstrong.sqlite.psi.core.psi.SqliteTypes
import com.intellij.lang.CodeDocumentationAwareCommenter
import com.intellij.psi.PsiComment
import com.intellij.psi.tree.IElementType

class SqlDelightCommenter : CodeDocumentationAwareCommenter {

  override fun getLineCommentTokenType(): IElementType = SqliteTypes.COMMENT
  override fun getLineCommentPrefix() = "-- "

  override fun getBlockCommentTokenType(): IElementType? = null
  override fun getBlockCommentPrefix(): String? = null
  override fun getBlockCommentSuffix(): String? = null

  override fun getDocumentationCommentTokenType(): IElementType = SqliteTypes.JAVADOC
  override fun isDocumentationComment(psiComment: PsiComment?) =
    psiComment?.tokenType == documentationCommentTokenType

  override fun getDocumentationCommentPrefix() = "/**"
  override fun getDocumentationCommentLinePrefix() = "*"
  override fun getDocumentationCommentSuffix() = "*/"

  override fun getCommentedBlockCommentPrefix(): String? = null
  override fun getCommentedBlockCommentSuffix(): String? = null
}
