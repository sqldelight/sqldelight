package app.cash.sqldelight.intellij.actions.generatemenu

import app.cash.sqldelight.core.lang.util.findChildOfType
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

fun SqlCreateTableStmt.getPrimaryKeyIndices() : List<Int> {
  val pkColumnIndex = columnDefList.indexOfFirst {  columnDef ->
    columnDef.columnConstraintList.any { columnConstraint ->
      columnConstraint.text.contains("PRIMARY KEY")
    }
  }

  return if (pkColumnIndex == -1) {
    val primaryKeyConstraint = tableConstraintList.firstOrNull { it.text.contains("PRIMARY KEY") }
    if (primaryKeyConstraint == null) {
      emptyList()
    } else {
      val primaryKeyColumns = primaryKeyConstraint.indexedColumnList.mapNotNull { it.findChildOfType<SqlColumnName>()?.name }.toSet()
      columnDefList.mapIndexedNotNull { index: Int, sqlColumnDef: SqlColumnDef ->
        if (primaryKeyColumns.contains(sqlColumnDef.columnName.name)) {
          index
        } else {
          null
        }
      }
    }
  } else {
    listOf(pkColumnIndex)
  }
}