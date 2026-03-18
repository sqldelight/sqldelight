package app.cash.sqldelight.dialects.mysql.grammar.mixins

import app.cash.sqldelight.dialects.mysql.grammar.psi.MySqlWindowFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.intellij.lang.ASTNode

internal abstract class WindowFunctionMixin(
  node: ASTNode,
) : SqlCompositeElementImpl(node),
  MySqlWindowFunctionExpr {
  val functionExpr get() = children.filterIsInstance<SqlFunctionExpr>().single()
}
