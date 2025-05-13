package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlAlterTableRenameColumn
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.AlterTableApplier
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.SqlColumnAlias
import com.alecstrong.sql.psi.core.psi.SqlColumnConstraint
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlColumnType
import com.alecstrong.sql.psi.core.psi.alterStmt
import com.intellij.lang.ASTNode

internal abstract class AlterTableRenameColumnMixin(
  node: ASTNode,
) : ColumnDefMixin(node),
  PostgreSqlAlterTableRenameColumn,
  AlterTableApplier {

  override fun getColumnConstraintList(): MutableList<SqlColumnConstraint> {
    return alterStmt.tablesAvailable(this).first { it.tableName.textMatches(alterStmt.tableName) }
      .query.columns.first { it.element.textMatches(columnName) }.element.let {
        (it.parent as SqlColumnDef).columnConstraintList
      }
  }

  override fun getColumnName(): SqlColumnName {
    return children.filterIsInstance<SqlColumnName>().first()
  }

  override fun getColumnType(): SqlColumnType {
    val sqlColumnType = children.filterIsInstance<SqlColumnType>().firstOrNull()
    if (sqlColumnType != null) return sqlColumnType

    val columnName = columnName
    val element = tablesAvailable(this).first { it.tableName.textMatches(alterStmt.tableName) }
      .query.columns.first { it.element.textMatches(columnName) }.element
    return (element.parent as ColumnDefMixin).columnType
  }

  private val columnAlias
    get() = children.filterIsInstance<SqlColumnAlias>().single()

  override fun applyTo(lazyQuery: LazyQuery): LazyQuery {
    return LazyQuery(
      tableName = lazyQuery.tableName,
      query = {
        val columns = lazyQuery.query.columns
        val replace = columns.singleOrNull {
          (it.element as NamedElement).textMatches(columnName)
        }
        lazyQuery.query.copy(
          columns = columns.map { if (it == replace) it.copy(columnAlias) else it },
        )
      },
    )
  }

  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    super.annotate(annotationHolder)

    if (tablesAvailable(this)
        .filter { it.tableName.textMatches(alterStmt.tableName) }
        .flatMap { it.query.columns }
        .none { (it.element as? NamedElement)?.textMatches(columnName) == true }
    ) {
      annotationHolder.createErrorAnnotation(
        element = columnName,
        message = "No column found to modify with name ${columnName.text}",
      )
    }
  }
}
