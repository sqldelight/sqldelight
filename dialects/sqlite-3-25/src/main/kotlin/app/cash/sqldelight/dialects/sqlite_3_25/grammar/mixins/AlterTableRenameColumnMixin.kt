package app.cash.sqldelight.dialects.sqlite_3_25.grammar.mixins

import app.cash.sqldelight.dialects.sqlite_3_25.grammar.psi.SqliteAlterTableRenameColumn
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.AlterTableApplier
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.SqlColumnAlias
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.alterStmt
import com.intellij.lang.ASTNode

internal abstract class AlterTableRenameColumnMixin(
  node: ASTNode
) : SqlCompositeElementImpl(node),
  SqliteAlterTableRenameColumn,
  AlterTableApplier {
  private val columnName
    get() = children.filterIsInstance<SqlColumnName>().single()

  private val columnAlias
    get() = children.filterIsInstance<SqlColumnAlias>().single()

  override fun applyTo(lazyQuery: LazyQuery): LazyQuery {
    return LazyQuery(
      tableName = lazyQuery.tableName,
      query = {
        val columns = lazyQuery.query.columns
        val column = QueryElement.QueryColumn(element = columnAlias)
        val replace = columns.singleOrNull {
          (it.element as SqlColumnName).textMatches(columnName)
        }
        lazyQuery.query.copy(
          columns = lazyQuery.query.columns.map { if (it == replace) column else it }
        )
      }
    )
  }

  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    super.annotate(annotationHolder)

    if (tablesAvailable(this)
      .filter { it.tableName.textMatches(alterStmt.tableName) }
      .flatMap { it.query.columns }
      .none { (it.element as? SqlColumnName)?.textMatches(columnName) == true }
    ) {
      annotationHolder.createErrorAnnotation(
        element = columnName,
        s = "No column found to modify with name ${columnName.text}"
      )
    }
  }
}
