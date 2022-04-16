package app.cash.sqldelight.dialects.sqlite_3_35.grammar.mixins

import app.cash.sqldelight.dialects.sqlite_3_35.grammar.psi.SqliteAlterTableDropColumn
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.AlterTableApplier
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCompositeElement
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlCreateIndexStmt
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.alecstrong.sql.psi.core.psi.alterStmt
import com.alecstrong.sql.psi.core.psi.mixins.ColumnDefMixin
import com.intellij.lang.ASTNode

internal abstract class AlterTableDropColumnMixin(
  node: ASTNode
) : SqlCompositeElementImpl(node),
  SqliteAlterTableDropColumn,
  AlterTableApplier {
  private val columnName
    get() = children.filterIsInstance<SqlColumnName>().single()

  override fun applyTo(lazyQuery: LazyQuery): LazyQuery {
    return LazyQuery(
      tableName = lazyQuery.tableName,
      query = {
        lazyQuery.query.copy(
          columns = lazyQuery.query.columns.filterNot { (it.element as SqlColumnName).textMatches(columnName) }
        )
      }
    )
  }

  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    super.annotate(annotationHolder)

    val columns = tablesAvailable(this)
      .filter { it.tableName.textMatches(alterStmt.tableName) }
      .flatMap { it.query.columns }

    val columnsToDrop = columns.filter { (it.element as? SqlColumnName)?.textMatches(columnName) == true }

    if (columns.size == 1) {
      annotationHolder.createErrorAnnotation(
        element = columnName,
        s = "Cannot drop column \"${columnName.text}\": no other columns exist"
      )
    } else {
      val constraints = columnsToDrop
        .mapNotNull { it.element.parent as? ColumnDefMixin }
        .flatMap { it.columnConstraintList }

      if (constraints.any { it.hasPrimaryKey() }) {
        annotationHolder.createErrorAnnotation(
          element = columnName,
          s = "Cannot drop PRIMARY KEY column \"${columnName.text}\""
        )
      } else if (constraints.any { it.isUnique() }) {
        annotationHolder.createErrorAnnotation(
          element = columnName,
          s = "Cannot drop UNIQUE column \"${columnName.text}\""
        )
      } else {
        containingFile
          .schema(SqlCreateIndexStmt::class, this)
          .find { index ->
            index.indexedColumnList.any { it.columnName?.textMatches(columnName) == true }
          }
          ?.let { indexForColumnToDrop ->
            annotationHolder.createErrorAnnotation(
              element = columnName,
              s = "Cannot drop indexed column \"${columnName.text}\" (\"${indexForColumnToDrop.indexName.text}\")"
            )
          }
      }
    }
  }

  companion object {
    private fun SqlCompositeElement.hasPrimaryKey() = node.findChildByType(SqlTypes.PRIMARY) != null
    private fun SqlCompositeElement.isUnique() = node.findChildByType(SqlTypes.UNIQUE) != null
  }
}
