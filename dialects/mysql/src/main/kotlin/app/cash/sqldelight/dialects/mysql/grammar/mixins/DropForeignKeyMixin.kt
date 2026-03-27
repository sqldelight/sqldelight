package app.cash.sqldelight.dialects.mysql.grammar.mixins

import app.cash.sqldelight.dialects.mysql.grammar.psi.MySqlAlterTableDropForeignKey
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.alecstrong.sql.psi.core.psi.SqlTableConstraint
import com.intellij.lang.ASTNode
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.parentOfType

internal abstract class DropForeignKeyMixin(
  node: ASTNode,
) : SqlCompositeElementImpl(node),
  MySqlAlterTableDropForeignKey {
  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    super.annotate(annotationHolder)
    val foreignKey = childrenOfType<SqlColumnName>().single()
    val foundColumnDef = foreignKey.getColumnDefOrNull()
    if (foundColumnDef == null || !foundColumnDef.isForeignKey()) {
      annotationHolder.createErrorAnnotation(
        foreignKey,
        "${foreignKey.name} is not a foreign key.",
      )
    }
  }

  private fun SqlColumnName.getColumnDefOrNull(): SqlColumnDef? {
    val ref = reference?.resolve() ?: return null
    val tables = tablesAvailable(this)
    for (table in tables) {
      val tableDef = table.tableName.parentOfType<SqlCreateTableStmt>() ?: continue
      for (columnDef in tableDef.columnDefList) {
        val columnRef = columnDef.columnName.reference
        if (columnRef != null && columnRef.resolve() == ref) {
          return columnDef
        }
      }
    }
    return null
  }

  private fun SqlColumnDef.isForeignKey(): Boolean {
    for (columnConstraint in columnConstraintList) {
      if (columnConstraint.foreignKeyClause != null) {
        return true
      }
    }
    val createTableStmt: SqlCreateTableStmt? = parentOfType()
    if (createTableStmt != null) {
      for (tableConstraints in createTableStmt.tableConstraintList) {
        val foreignKeyClause = tableConstraints.foreignKeyClause
        if (foreignKeyClause != null) {
          val columns = (foreignKeyClause.parent as SqlTableConstraint).columnNameList
          for (column in columns) {
            if (column.reference?.resolve() == columnName) {
              return true
            }
          }
        }
      }
    }
    return false
  }
}
