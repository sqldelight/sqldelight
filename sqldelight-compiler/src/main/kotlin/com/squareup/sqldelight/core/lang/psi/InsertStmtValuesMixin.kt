package com.squareup.sqldelight.core.lang.psi

import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.impl.SqlInsertStmtValuesImpl
import com.alecstrong.sql.psi.core.psi.mixins.ColumnDefMixin
import com.intellij.lang.ASTNode
import com.squareup.sqldelight.core.lang.acceptsTableInterface
import com.squareup.sqldelight.core.psi.SqlDelightInsertStmtValues

open class InsertStmtValuesMixin(
  node: ASTNode
) : SqlInsertStmtValuesImpl(node),
    SqlDelightInsertStmtValues {
  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    val parent = parent ?: return
    if (parent.acceptsTableInterface()) {
      val table = tableAvailable(this, parent.tableName.name).firstOrNull() ?: return
      val columns = table.columns.map { (it.element as SqlColumnName).name }
      val setColumns =
        if (parent.columnNameList.isEmpty()) {
          columns
        } else {
          parent.columnNameList.mapNotNull { it.name }
        }

      val needsDefaultValue = table.columns
          .filter { (element, _) -> element is SqlColumnName &&
              element.name !in setColumns &&
              !(element.parent as ColumnDefMixin).hasDefaultValue()
          }
          .map { it.element as SqlColumnName }
      if (needsDefaultValue.size == 1) {
        annotationHolder.createErrorAnnotation(parent, "Cannot populate default value for column " +
            "${needsDefaultValue.first().name}, it must be specified in insert statement.")
      } else if (needsDefaultValue.size > 1) {
        annotationHolder.createErrorAnnotation(parent, "Cannot populate default values for columns " +
            "(${needsDefaultValue.joinToString { it.name }}), they must be specified in insert statement.")
      }

      // This is going to break error handling in sqlite-psi so just omit the superclass annotator.
      return
    }

    super.annotate(annotationHolder)
  }
}
