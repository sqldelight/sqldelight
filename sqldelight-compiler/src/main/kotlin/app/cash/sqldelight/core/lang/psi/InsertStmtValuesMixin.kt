package app.cash.sqldelight.core.lang.psi

import app.cash.sqldelight.core.lang.acceptsTableInterface
import app.cash.sqldelight.core.lang.util.columnDefSource
import app.cash.sqldelight.core.psi.SqlDelightInsertStmtValues
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.impl.SqlInsertStmtValuesImpl
import com.intellij.lang.ASTNode

open class InsertStmtValuesMixin(
  node: ASTNode
) : SqlInsertStmtValuesImpl(node),
  SqlDelightInsertStmtValues {
  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    val parent = parent ?: return
    if (parent.acceptsTableInterface()) {
      val table = tableAvailable(this, parent.tableName.name).firstOrNull() ?: return
      val columns = table.columns.map { (it.element as NamedElement).name }
      val setColumns =
        if (parent.columnNameList.isEmpty()) {
          columns
        } else {
          parent.columnNameList.mapNotNull { it.name }
        }

      val needsDefaultValue = table.columns
        .filter { (element, _) ->
          element is NamedElement &&
            element.name !in setColumns &&
            !(element.columnDefSource()?.hasDefaultValue() ?: return)
        }
        .map { it.element as NamedElement }
      if (needsDefaultValue.size == 1) {
        annotationHolder.createErrorAnnotation(
          parent,
          "Cannot populate default value for column " +
            "${needsDefaultValue.first().name}, it must be specified in insert statement."
        )
      } else if (needsDefaultValue.size > 1) {
        annotationHolder.createErrorAnnotation(
          parent,
          "Cannot populate default values for columns " +
            "(${needsDefaultValue.joinToString { it.name }}), they must be specified in insert statement."
        )
      }

      // This is going to break error handling in sqlite-psi so just omit the superclass annotator.
      return
    }

    super.annotate(annotationHolder)
  }
}
