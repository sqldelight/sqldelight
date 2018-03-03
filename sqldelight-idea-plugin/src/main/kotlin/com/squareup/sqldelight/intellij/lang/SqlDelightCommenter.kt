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
