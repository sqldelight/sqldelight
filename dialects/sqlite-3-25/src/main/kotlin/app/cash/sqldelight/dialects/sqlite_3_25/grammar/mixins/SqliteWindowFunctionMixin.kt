package app.cash.sqldelight.dialects.sqlite_3_25.grammar.mixins

import app.cash.sqldelight.dialects.sqlite_3_25.grammar.psi.SqliteWindowFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlFunctionName
import com.intellij.lang.ASTNode

internal abstract class SqliteWindowFunctionMixin(
  node: ASTNode,
) : SqlCompositeElementImpl(node),
  SqliteWindowFunctionExpr,
  SqlFunctionExpr {
  override fun getExprList(): List<SqlExpr> {
    return children.filterIsInstance<SqlFunctionExpr>()
  }

  override fun getFunctionName(): SqlFunctionName {
    return exprList.first().children.filterIsInstance<SqlFunctionName>().single()
  }
}
