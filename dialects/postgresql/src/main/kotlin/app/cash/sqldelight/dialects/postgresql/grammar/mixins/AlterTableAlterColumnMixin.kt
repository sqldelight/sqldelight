package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlAlterTableAlterColumn
import com.alecstrong.sql.psi.core.psi.AliasElement
import com.alecstrong.sql.psi.core.psi.AlterTableApplier
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.SqlColumnConstraint
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlColumnType
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.alecstrong.sql.psi.core.psi.impl.SqlColumnDefImpl
import com.alecstrong.sql.psi.core.psi.mixins.ColumnDefMixin
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

internal abstract class AlterTableAlterColumnMixin(
  node: ASTNode,
) : SqlColumnDefImpl(node),
  PostgreSqlAlterTableAlterColumn,
  AlterTableApplier {

  override fun getColumnConstraintList(): MutableList<SqlColumnConstraint> {
    return (
      (
        tablesAvailable(this).first()
          .query.columns.first { it.element.textMatches(columnName) }.element as NamedElement
        ).columnDefSource()!!.columnConstraintList
      )
  }

  override fun getColumnName(): SqlColumnName {
    return children.filterIsInstance<SqlColumnName>().first()
  }

  override fun getColumnType(): SqlColumnType {
    return children.filterIsInstance<SqlColumnType>().firstOrNull()
      ?: return (
        (
          tablesAvailable(this).first()
            .query.columns.first { it.element.textMatches(columnName) }.element as NamedElement
          ).columnDefSource()!!.columnType
        )
  }

  override fun getJavadoc(): PsiElement? {
    return (
      (
        tablesAvailable(this).first()
          .query.columns.first { it.element.textMatches(columnName) }.element as NamedElement
        ).columnDefSource()!!.javadoc
      )
  }

  private fun NamedElement.columnDefSource(): ColumnDefMixin? {
    if (this.parent is ColumnDefMixin) return this.parent as ColumnDefMixin
    if (this is AliasElement) return (source() as NamedElement).columnDefSource()
    if (this is SqlColumnName) return (reference!!.resolve() as NamedElement).columnDefSource()
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

    val alterColumnTable = getColumnName()
    return LazyQuery(
      tableName = lazyQuery.tableName,
      query = {
        val columns = lazyQuery.query.columns.map { queryColumn ->
          if (queryColumn.element.textMatches(alterColumnTable)) queryColumn.copy(element = alterColumnTable, nullable = nullableColumn) else queryColumn
        }
        lazyQuery.query.copy(columns = columns)
      },
    )
  }
}
