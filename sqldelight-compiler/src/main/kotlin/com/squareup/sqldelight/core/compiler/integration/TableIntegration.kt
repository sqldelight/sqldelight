package com.squareup.sqldelight.core.compiler.integration

import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.SqlAlterTableStmt
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.alecstrong.sql.psi.core.psi.SqlCreateViewStmt
import com.alecstrong.sql.psi.core.psi.SqlCreateVirtualTableStmt
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler
import com.squareup.sqldelight.core.lang.ADAPTER_NAME
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.psi.ColumnTypeMixin

internal fun LazyQuery.needsAdapters() = when (tableName.parent) {
  is SqlCreateViewStmt -> false
  else -> columns().any { (it.columnType as ColumnTypeMixin).adapter() != null }
}

internal fun LazyQuery.adapterProperty(): PropertySpec {
  val adapterType = ClassName(
      (tableName.containingFile as SqlDelightFile).packageName!!,
      SqlDelightCompiler.allocateName(tableName).capitalize(),
      ADAPTER_NAME
  )
  return PropertySpec.builder(adapterName, adapterType, KModifier.INTERNAL)
      .initializer(adapterName)
      .build()
}

private fun LazyQuery.columns() = when (val parentRule = tableName.parent) {
  is SqlCreateTableStmt -> parentRule.columnDefList
  is SqlAlterTableStmt, is SqlCreateVirtualTableStmt -> query.columns.map { it.element.parent as SqlColumnDef }
  else -> throw IllegalStateException("Unexpected query parent $parentRule")
}

internal val LazyQuery.adapterName
  get() = "${SqlDelightCompiler.allocateName(tableName)}$ADAPTER_NAME"
