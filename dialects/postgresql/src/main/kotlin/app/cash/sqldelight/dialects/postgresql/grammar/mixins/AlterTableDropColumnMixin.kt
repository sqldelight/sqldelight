package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlAlterTableDropColumn
import com.alecstrong.sql.psi.core.psi.AlterTableApplier
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.intellij.lang.ASTNode

internal abstract class AlterTableDropColumnMixin(
  node: ASTNode
) : SqlCompositeElementImpl(node),
  PostgreSqlAlterTableDropColumn,
  AlterTableApplier {
  private val columnName
    get() = children.filterIsInstance<SqlColumnName>().single()

  override fun applyTo(lazyQuery: LazyQuery): LazyQuery {
    return LazyQuery(
      tableName = lazyQuery.tableName,
      query = {
        val columns = lazyQuery.query.columns.filter { it.element.text != columnName.name }
        lazyQuery.query.copy(columns = columns)
      }
    )
  }
}
