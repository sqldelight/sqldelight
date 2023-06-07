package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlWindowFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.intellij.lang.ASTNode

internal abstract class WindowFunctionMixin(
  node: ASTNode,
) : SqlCompositeElementImpl(node),
  PostgreSqlWindowFunctionExpr {
  val functionExpr get() = children.filterIsInstance<SqlFunctionExpr>().single()
}
