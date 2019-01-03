package com.squareup.sqldelight.core.compiler.model

import com.intellij.psi.PsiElement
import com.squareup.sqldelight.core.lang.psi.StmtIdentifierMixin

open class NamedExecute(
  identifier: StmtIdentifierMixin,
  statement: PsiElement
) : BindableQuery(identifier, statement) {
  val name = identifier.name!!
}