package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlAtTimeZoneOperator
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.intellij.lang.ASTNode

/**
 *
 */
internal abstract class AtTimeZoneOperatorMixin(node: ASTNode) :
  SqlCompositeElementImpl(node),
  SqlExpr,
  PostgreSqlAtTimeZoneOperator
