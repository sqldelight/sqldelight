package com.squareup.sqldelight.core.lang.util

import com.alecstrong.sqlite.psi.core.psi.SqliteCreateTableStmt
import com.squareup.kotlinpoet.ClassName
import com.squareup.sqldelight.core.psi.SqlDelightColumnDef

internal val SqliteCreateTableStmt.columns: List<SqlDelightColumnDef>
  get() = columnDefList.filterIsInstance<SqlDelightColumnDef>()

internal val SqliteCreateTableStmt.interfaceType: ClassName
  get() = ClassName(sqFile().packageName, tableName.name.capitalize())
