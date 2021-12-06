package app.cash.sqldelight.core.lang.util

import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlInsertStmt

/**
 * The list of columns that values are being provided for.
 */
internal val SqlInsertStmt.columns: List<SqlColumnDef>
  get() {
    val columns = table.query.columns
      .mapNotNull { (it.element as? SqlColumnName)?.parent as? SqlColumnDef }
    if (columnNameList.isEmpty()) return columns

    val columnMap = linkedMapOf(*columns.map { it.columnName.name to it }.toTypedArray())
    return columnNameList.mapNotNull { columnMap[it.name] }
  }

internal val SqlInsertStmt.table: LazyQuery
  get() = tablesAvailable(this).first { it.tableName.name == tableName.name }
