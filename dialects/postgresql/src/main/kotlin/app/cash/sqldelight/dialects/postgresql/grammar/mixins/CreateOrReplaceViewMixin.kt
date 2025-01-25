package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.impl.SqlCreateViewStmtImpl
import com.intellij.lang.ASTNode

/**
 * See sql-psi com.alecstrong.sql.psi.core.psi.mixins.CreateViewMixin where `REPLACE` is enabled
 * Add annotations to check replace has identical set of columns in same order, but allows appending columns
 */
internal abstract class CreateOrReplaceViewMixin(
  node: ASTNode,
) : SqlCreateViewStmtImpl(node) {

  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    val currentColumns: List<QueryElement.QueryColumn> = tableAvailable(this, viewName.name).flatMap { it.columns }
    val newColumns = compoundSelectStmt!!.queryExposed().flatMap { it.columns }
    if (currentColumns.size > newColumns.size) {
      annotationHolder.createErrorAnnotation(this, "Cannot drop columns from ${viewName.name}")
    }

    currentColumns.zip(newColumns).firstOrNull { (current, new) -> current != new }?.let { (current, new) ->
      annotationHolder.createErrorAnnotation(this, """Cannot change name of view column "${current.element.text}" to "${new.element.text}"""")
    }
    super.annotate(annotationHolder)
  }
}
