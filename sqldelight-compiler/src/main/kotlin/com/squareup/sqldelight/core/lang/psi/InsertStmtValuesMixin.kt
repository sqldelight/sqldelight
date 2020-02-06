package com.squareup.sqldelight.core.lang.psi

import com.alecstrong.sqlite.psi.core.SqliteAnnotationHolder
import com.alecstrong.sqlite.psi.core.hasDefaultValue
import com.alecstrong.sqlite.psi.core.psi.SqliteColumnDef
import com.alecstrong.sqlite.psi.core.psi.SqliteColumnName
import com.alecstrong.sqlite.psi.core.psi.impl.SqliteInsertStmtValuesImpl
import com.intellij.lang.ASTNode
import com.squareup.sqldelight.core.lang.acceptsTableInterface
import com.squareup.sqldelight.core.psi.SqlDelightInsertStmtValues

open class InsertStmtValuesMixin(
  node: ASTNode
) : SqliteInsertStmtValuesImpl(node),
    SqlDelightInsertStmtValues {
  override fun annotate(annotationHolder: SqliteAnnotationHolder) {
    if (parent.acceptsTableInterface()) {
      val table = tableAvailable(this, parent.tableName.name).firstOrNull() ?: return
      val columns = table.columns.map { (it.element as SqliteColumnName).name }
      val setColumns =
        if (parent.columnNameList.isEmpty()) {
          columns
        } else {
          parent.columnNameList.mapNotNull { it.name }
        }

      val needsDefaultValue = table.columns
          .filter { (element, _) -> element is SqliteColumnName
              && element.name !in setColumns
              && !(element.parent as SqliteColumnDef).hasDefaultValue()
          }
          .map { it.element as SqliteColumnName }
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