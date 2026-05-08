package app.cash.sqldelight.core.lang.util

import com.alecstrong.sql.psi.core.psi.AliasElement
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.alecstrong.sql.psi.core.psi.SqlCreateVirtualTableStmt
import com.alecstrong.sql.psi.core.psi.mixins.ColumnDefMixin

internal fun <T> Iterable<T>.filterCodegenExcludedColumns(
  columnOf: (T) -> NamedElement?,
): List<T> {
  return filterNot { columnOf(it)?.isExcludedFromCodegen() == true }
}

internal fun NamedElement.isExcludedFromCodegen(): Boolean {
  val excludedColumns = sqFile().codegenExcludedColumns
  if (excludedColumns.isEmpty()) return false

  val columnDef = columnDefSourceForCodegenExclusion() ?: return false
  val tableName = when (val parent = columnDef.parent) {
    is SqlCreateTableStmt -> parent.tableName.name
    is SqlCreateVirtualTableStmt -> parent.tableName.name
    else -> null
  } ?: return false
  val columnName = columnDef.columnName.name

  return excludedColumns.any { excludedColumn ->
    excludedColumn.toCodegenExcludedColumnSpec()?.matches(tableName, columnName) == true
  }
}

private fun NamedElement.columnDefSourceForCodegenExclusion(): ColumnDefMixin? {
  return columnDefSourceForCodegenExclusion(mutableSetOf())
}

private fun NamedElement.columnDefSourceForCodegenExclusion(seen: MutableSet<NamedElement>): ColumnDefMixin? {
  if (!seen.add(this)) return null
  if (parent is ColumnDefMixin) return parent as ColumnDefMixin
  if (this is AliasElement) return (source() as? NamedElement)?.columnDefSourceForCodegenExclusion(seen)
  if (this is SqlColumnName) return (reference?.resolve() as? NamedElement)?.columnDefSourceForCodegenExclusion(seen)
  return null
}

private fun String.toCodegenExcludedColumnSpec(): CodegenExcludedColumnSpec? {
  val trimmed = trim()
  val splitIndex = trimmed.indexOf('.')
  if (splitIndex <= 0 || splitIndex == trimmed.lastIndex || trimmed.indexOf('.', splitIndex + 1) != -1) {
    return null
  }
  return CodegenExcludedColumnSpec(
    tableName = trimmed.substring(0, splitIndex),
    columnName = trimmed.substring(splitIndex + 1),
  )
}

private data class CodegenExcludedColumnSpec(
  val tableName: String,
  val columnName: String,
) {
  fun matches(tableName: String, columnName: String): Boolean {
    return this.tableName.equals(tableName, ignoreCase = true) &&
      this.columnName.equals(columnName, ignoreCase = true)
  }
}
