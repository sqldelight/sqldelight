package app.cash.sqldelight.core.lang.util

import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryColumn
import com.alecstrong.sql.psi.core.psi.SqlInsertStmt

internal val SqlInsertStmt.queryColumns: List<QueryColumn>
  get() {
    val columns = table.query.columns
      .filterCodegenExcludedColumns { it.element as? NamedElement }
    if (columnNameList.isEmpty()) return columns

    val columnMap = linkedMapOf(*columns.map { (it.element as NamedElement).name to it }.toTypedArray())
    return columnNameList.mapNotNull { columnMap[it.name] }
  }

/**
 * The list of columns that values are being provided for.
 */
internal val SqlInsertStmt.columns: List<NamedElement>
  get() = queryColumns.map { it.element as NamedElement }

internal val SqlInsertStmt.table: LazyQuery
  get() = tablesAvailable(this).first { it.tableName.name == tableName.name }
