package app.cash.sqldelight.core.lang.psi

import app.cash.sqldelight.core.lang.util.sqFile
import app.cash.sqldelight.core.psi.SqlDelightStmtClojureStmtList
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

abstract class ClojureStmtListMixin(
  node: ASTNode
) : SqlCompositeElementImpl(node),
  SqlDelightStmtClojureStmtList {
  override fun tablesAvailable(child: PsiElement): Collection<LazyQuery> {
    return sqFile().tablesAvailable(child)
  }
}
