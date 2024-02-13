package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlAlterTableAddConstraint
import com.alecstrong.sql.psi.core.psi.AlterTableApplier
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.lang.ASTNode

abstract class AlterTableAddConstraintMixin(node: ASTNode) :
  SqlCompositeElementImpl(node),
  PostgreSqlAlterTableAddConstraint,
  AlterTableApplier {
  override fun applyTo(lazyQuery: LazyQuery): LazyQuery =
    if (tableConstraint.node.findChildByType(SqlTypes.PRIMARY) != null &&
      tableConstraint.node.findChildByType(SqlTypes.KEY) != null
    ) {
      val columns = lazyQuery.query.columns.map { queryCol ->
        tableConstraint.indexedColumnList.find { indexedCol -> queryCol.element.textMatches(indexedCol) }.let {
          queryCol.copy(nullable = if (it != null) false else queryCol.nullable)
        }
      }
      LazyQuery(lazyQuery.tableName, query = { lazyQuery.query.copy(columns = columns) })
    } else {
      lazyQuery
    }
}
