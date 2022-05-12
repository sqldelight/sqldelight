package app.cash.sqldelight.dialects.mysql.grammar.mixins

import app.cash.sqldelight.dialects.mysql.grammar.psi.MySqlAlterTableAddColumn
import com.alecstrong.sql.psi.core.psi.AlterTableApplier
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.impl.SqlAlterTableAddColumnImpl
import com.intellij.lang.ASTNode

internal abstract class AlterTableAddColumnMixin(
  node: ASTNode
) : SqlAlterTableAddColumnImpl(node),
  MySqlAlterTableAddColumn,
  AlterTableApplier {
  override fun applyTo(lazyQuery: LazyQuery): LazyQuery {
    return LazyQuery(
      tableName = lazyQuery.tableName,
      query = {
        val columns = placementClause.placeInQuery(
          columns = lazyQuery.query.columns,
          column = QueryElement.QueryColumn(columnDef.columnName)
        )
        lazyQuery.query.copy(columns = columns)
      }
    )
  }
}
