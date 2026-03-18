package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlArrayValueExpression
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.intellij.lang.ASTNode

/**
 * Array value expressions:
 * SELECT ARRAY[1, 2, 3] // integer[]
 * SELECT ARRAY[1.1, 2.2, 3.3] // numeric[]
 * SELECT ARRAY['a', 'b', 'c'] // text[]
 * SELECT ARRAY[TRUE, FALSE] // boolean[]
 * SELECT ARRAY[1.0::NUMERIC, NULL, 3::LONG] // numeric[]
 *
 * Note: SELECT ARRAY[DATE '2020-01-01', DATE '2020-01-02'] // date[] LocalDate is not supported in PostgreSQL arrays
 *
 * Contains a list of expressions inside the brackets for limited type resolution.
 */
internal abstract class ArrayValueExpressionMixin(node: ASTNode) :
  SqlCompositeElementImpl(node),
  SqlExpr,
  PostgreSqlArrayValueExpression {

  fun exprList(): List<SqlExpr> {
    return children.filterIsInstance<SqlExpr>()
  }
}
