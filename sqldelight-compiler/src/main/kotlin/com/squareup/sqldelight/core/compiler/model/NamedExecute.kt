package com.squareup.sqldelight.core.compiler.model

import com.intellij.psi.PsiElement
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.sqldelight.core.lang.STATEMENT_TYPE_ENUM
import com.squareup.sqldelight.core.lang.psi.StmtIdentifierMixin

open class NamedExecute(
  identifier: StmtIdentifierMixin,
  statement: PsiElement
) : BindableQuery(identifier, statement) {
  val name = identifier.name!!

  override fun type(): CodeBlock {
    return CodeBlock.of("%T.EXECUTE", STATEMENT_TYPE_ENUM)
  }
}