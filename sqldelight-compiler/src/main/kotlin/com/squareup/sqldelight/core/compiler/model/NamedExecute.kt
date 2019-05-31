package com.squareup.sqldelight.core.compiler.model

import com.intellij.psi.PsiElement
import com.squareup.sqldelight.core.lang.psi.StmtIdentifierMixin

open class NamedExecute(
  private val id: String,
  identifier: StmtIdentifierMixin,
  statement: PsiElement
) : BindableQuery(identifier, statement) {
  val name = identifier.name!!

  override fun getQueryId() = id
}