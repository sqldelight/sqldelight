package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlAlterTableAlterColumn
import com.alecstrong.sql.psi.core.psi.AlterTableApplier
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.intellij.lang.ASTNode

internal abstract class AlterTableAlterColumnMixin(
  node: ASTNode,
) : SqlCompositeElementImpl(node),
  PostgreSqlAlterTableAlterColumn,
  AlterTableApplier {
  override fun applyTo(lazyQuery: LazyQuery): LazyQuery {
    return lazyQuery
  }
}
