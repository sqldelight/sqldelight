package com.squareup.sqldelight.core.lang.psi

import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.squareup.sqldelight.core.lang.util.sqFile
import com.squareup.sqldelight.core.psi.SqlDelightStmtClojureStmtList

abstract class ClojureStmtListMixin(
  node: ASTNode
) : SqlCompositeElementImpl(node),
  SqlDelightStmtClojureStmtList {
  override fun tablesAvailable(child: PsiElement): Collection<LazyQuery> {
    return sqFile().tablesAvailable(child)
  }
}
