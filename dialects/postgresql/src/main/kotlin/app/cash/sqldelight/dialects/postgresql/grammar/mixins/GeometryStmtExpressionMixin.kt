package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlGeometryPointFunctionStmt
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.intellij.lang.ASTNode

// Make the GeometryStmt a SqlExpr so it can be matched in `app.cash.sqldelight.dialects.postgresql.PostgreSqlTypeResolver.argumentType`
// setting the bind argument type to PostgreSqlGeometryPointFunctionStmt REAL or PostgreSqlGeometrySetsridFunctionStmt INTEGER
internal abstract class GeometryStmtExpressionMixin(
  node: ASTNode,
) : SqlCompositeElementImpl(node),
  SqlExpr,
  PostgreSqlGeometryPointFunctionStmt
