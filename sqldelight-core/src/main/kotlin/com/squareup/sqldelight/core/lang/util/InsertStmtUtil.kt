package com.squareup.sqldelight.core.lang.util

import com.alecstrong.sqlite.psi.core.psi.LazyQuery
import com.alecstrong.sqlite.psi.core.psi.SqliteColumnName
import com.alecstrong.sqlite.psi.core.psi.SqliteInsertStmt
import com.squareup.sqldelight.core.psi.SqlDelightColumnDef

/**
 * The list of columns that values are being provided for.
 */
internal val SqliteInsertStmt.columns: List<SqlDelightColumnDef>
  get() {
    val columns = table.query().columns
        .filterIsInstance<SqliteColumnName>()
        .map { it.parent as SqlDelightColumnDef }
    if (columnNameList.isEmpty()) return columns

    val columnMap = linkedMapOf(*columns.map { it.columnName.name to it }.toTypedArray())
    return columnNameList.mapNotNull { columnMap[it.name] }
  }

internal val SqliteInsertStmt.table: LazyQuery
  get() = tablesAvailable(this).first { it.tableName.name == tableName?.name }
