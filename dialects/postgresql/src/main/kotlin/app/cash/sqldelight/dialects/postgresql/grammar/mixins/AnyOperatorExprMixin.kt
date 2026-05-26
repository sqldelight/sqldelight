package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.intellij.lang.ASTNode

internal abstract class AnyOperatorExprMixin(
  node: ASTNode,
) : SqlCompositeElementImpl(node),
  SqlExpr {
  val expr get() = children.filterIsInstance<SqlExpr>().first()
}
