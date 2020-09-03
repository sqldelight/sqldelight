package com.squareup.sqldelight.core.compiler.model

import com.intellij.psi.PsiElement
import com.squareup.sqldelight.core.lang.psi.StmtIdentifierMixin
import com.squareup.sqldelight.core.lang.util.sqFile

open class NamedExecute(
  identifier: StmtIdentifierMixin,
  statement: PsiElement
) : BindableQuery(identifier, statement) {
  val name = identifier.name!!

  override val id: Int
    // the sqlFile package name -> com.example.
    // sqlFile.name -> test.sq
    // name -> query name
    get() = idForIndex(null)

  internal fun idForIndex(index: Int?): Int {
    val postFix = if (index == null) "" else "_$index"
    return getUniqueQueryIdentifier(statement.sqFile().let {
      "${it.packageName}:${it.name}:$name$postFix"
    })
  }
}
