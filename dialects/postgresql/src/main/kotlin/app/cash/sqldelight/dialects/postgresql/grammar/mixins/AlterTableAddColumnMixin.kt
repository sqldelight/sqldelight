package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlAlterTableAddColumn
import com.alecstrong.sql.psi.core.psi.AlterTableApplier
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.SqlAlterTableAddColumn
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil

internal abstract class AlterTableAddColumnMixin(
  node: ASTNode,
) : SqlCompositeElementImpl(node),
  SqlAlterTableAddColumn,
  PostgreSqlAlterTableAddColumn,
  AlterTableApplier {
  override fun applyTo(lazyQuery: LazyQuery): LazyQuery {
    return LazyQuery(
      tableName = lazyQuery.tableName,
      query = {
        val columns = lazyQuery.query.columns
        val existingColumn = columns.singleOrNull {
          (it.element as NamedElement).textMatches(columnDef.columnName)
        }

        lazyQuery.query.copy(
          columns = if (ifNotExists != null && existingColumn != null) lazyQuery.query.columns else columns + QueryElement.QueryColumn(columnDef.columnName),
        )
      },
    )
  }

  override fun getColumnDef(): SqlColumnDef {
    return notNullChild(PsiTreeUtil.getChildOfType(this, SqlColumnDef::class.java))
  }
}
