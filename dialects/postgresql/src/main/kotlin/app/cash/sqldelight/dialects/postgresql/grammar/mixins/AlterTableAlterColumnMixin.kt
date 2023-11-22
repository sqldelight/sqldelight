package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlAlterTableAlterColumn
import com.alecstrong.sql.psi.core.psi.AlterTableApplier
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.util.elementType

internal abstract class AlterTableAlterColumnMixin(
  node: ASTNode,
) : SqlCompositeElementImpl(node),
  PostgreSqlAlterTableAlterColumn,
  AlterTableApplier {

  private val columnName
    get() = children.filterIsInstance<SqlColumnName>().first()

  override fun applyTo(lazyQuery: LazyQuery): LazyQuery {
    val hasDropNotNull = columnNotNullClause?.let { it.firstChild.elementType == SqlTypes.DROP }
    val hasSetNotNull = columnNotNullClause?.let { it.firstChild.elementType == SqlTypes.SET }

    return LazyQuery(
      tableName = lazyQuery.tableName,
      query = {
        val columns = lazyQuery.query.columns.map {
          if (it.element.textMatches(columnName)) {
            when {
              (hasDropNotNull == true) -> it.copy(nullable = true)
              (hasSetNotNull == true) -> it.copy(nullable = false)
              else -> it
            }
          } else {
            it
          }
        }

        lazyQuery.query.copy(columns = columns)
      },
    )
  }
}
