package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlAlterTableAlterColumn
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.AlterTableApplier
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.SqlColumnConstraint
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlColumnType
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.alecstrong.sql.psi.core.psi.alterStmt
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

internal abstract class AlterTableAlterColumnMixin(
  node: ASTNode,
) : ColumnDefMixin(node),
  PostgreSqlAlterTableAlterColumn,
  AlterTableApplier {

  override fun getColumnConstraintList(): MutableList<SqlColumnConstraint> {
    return alterStmt.tablesAvailable(this).first { it.tableName.textMatches(alterStmt.tableName) }
      .query.columns.first { it.element.textMatches(columnName) }.element.let {
        (it.parent as SqlColumnDef).columnConstraintList
      }
  }

  override fun hasDefaultValue(): Boolean {
    val defaultColumn: Boolean? = columnDefaultClause?.let {
      when (it.firstChild.elementType) {
        SqlTypes.DROP -> false
        SqlTypes.SET -> true
        else -> null
      }
    }

    return defaultColumn ?: super.hasDefaultValue()
  }

  override fun getColumnName(): SqlColumnName {
    return children.filterIsInstance<SqlColumnName>().first()
  }

  override fun getColumnType(): SqlColumnType {
    val sqlColumnType = children.filterIsInstance<SqlColumnType>().singleOrNull()
    if (sqlColumnType != null) return sqlColumnType

    val element = tablesAvailable(this).first { it.tableName.textMatches(alterStmt.tableName) }
      .query.columns.first { it.element.textMatches(columnName) }.element
    return (element.parent as ColumnDefMixin).columnType
  }

  override fun getJavadoc(): PsiElement? {
    return null
  }

  override fun applyTo(lazyQuery: LazyQuery): LazyQuery {
    val nullableColumn: Boolean? = columnNotNullClause?.let {
      when (it.firstChild.elementType) {
        SqlTypes.DROP -> true
        SqlTypes.SET -> false
        else -> null
      }
    }

    return LazyQuery(
      tableName = lazyQuery.tableName,
      query = {
        val columns = lazyQuery.query.columns
        val alterColumn = columns.singleOrNull {
          (it.element as NamedElement).textMatches(columnName)
        }

        val sqlColumnName = children.filterIsInstance<SqlColumnType>().singleOrNull()?.run { columnName }

        lazyQuery.query.copy(
          columns = columns.map {
            if (it == alterColumn) it.copy(element = sqlColumnName ?: it.element, nullable = nullableColumn ?: it.nullable) else it
          },
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
        message = "No column found to alter with name ${columnName.text}",
      )
    }
  }
}
