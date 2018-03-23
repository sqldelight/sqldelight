package com.squareup.sqldelight.core.lang.psi

import com.alecstrong.sqlite.psi.core.SqliteAnnotationHolder
import com.alecstrong.sqlite.psi.core.psi.SqliteColumnDef
import com.alecstrong.sqlite.psi.core.psi.SqliteColumnName
import com.alecstrong.sqlite.psi.core.psi.SqliteTypes
import com.alecstrong.sqlite.psi.core.psi.impl.SqliteInsertStmtImpl
import com.intellij.lang.ASTNode
import com.squareup.sqldelight.core.lang.util.childOfType
import com.squareup.sqldelight.core.psi.SqlDelightInsertStmt

open class InsertStmtMixin(
  node: ASTNode
) : SqliteInsertStmtImpl(node),
    SqlDelightInsertStmt {
  override fun annotate(annotationHolder: SqliteAnnotationHolder) {
    if (acceptsTableInterface()) {
      val table = tableAvailable(this, tableName.name).firstOrNull() ?: return
      val columns = table.columns.map { (it.element as SqliteColumnName).name }
      val setColumns =
        if (columnNameList.isEmpty()) {
          columns
        } else {
          columnNameList.mapNotNull { it.name }
        }

      val needsDefaultValue = table.columns
          .filter { (element, _) -> element is SqliteColumnName
              && element.name !in setColumns
              && !(element.parent as SqliteColumnDef).hasDefaultValue()
          }
          .map { it.element as SqliteColumnName }
      if (needsDefaultValue.size == 1) {
        annotationHolder.createErrorAnnotation(this, "Cannot populate default value for column " +
            "${needsDefaultValue.first().name}, it must be specified in insert statement.")
      } else if (needsDefaultValue.size > 1) {
        annotationHolder.createErrorAnnotation(this, "Cannot populate default values for columns " +
            "(${needsDefaultValue.joinToString { it.name }}), they must be specified in insert statement.")
      }

      // This is going to break error handling in sqlite-psi so just omit the superclass annotator.
      return
    }

    super.annotate(annotationHolder)
  }

  internal fun acceptsTableInterface(): Boolean {
    return childOfType(SqliteTypes.BIND_EXPR) != null
  }
}