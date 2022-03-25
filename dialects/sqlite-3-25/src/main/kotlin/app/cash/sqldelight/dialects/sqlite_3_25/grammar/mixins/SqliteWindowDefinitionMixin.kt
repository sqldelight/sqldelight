package app.cash.sqldelight.dialects.sqlite_3_25.grammar.mixins

import com.alecstrong.sql.psi.core.psi.FromQuery
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType

abstract class SqliteWindowDefinitionMixin(node: ASTNode) : SqlCompositeElementImpl(node) {
  override fun queryAvailable(child: PsiElement): Collection<QueryElement.QueryResult> {
    return parentOfType<FromQuery>()!!.fromQuery()
  }
}
