package app.cash.sqldelight.core.compiler.integration

import app.cash.sqldelight.core.capitalize
import app.cash.sqldelight.core.compiler.SqlDelightCompiler
import app.cash.sqldelight.core.lang.ADAPTER_NAME
import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.psi.ColumnTypeMixin
import app.cash.sqldelight.core.lang.util.columnDefSource
import app.cash.sqldelight.core.lang.util.filterCodegenExcludedColumns
import app.cash.sqldelight.core.lang.util.isExcludedFromCodegen
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.alecstrong.sql.psi.core.psi.SqlCreateViewStmt
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec

internal fun LazyQuery.needsAdapters() = when (tableName.parent) {
  is SqlCreateViewStmt -> false
  else -> columns().any { (it.columnType as ColumnTypeMixin).adapter() != null }
}

internal fun LazyQuery.adapterProperty(): PropertySpec {
  val adapterType = ClassName(
    (tableName.containingFile as SqlDelightFile).packageName!!,
    SqlDelightCompiler.allocateName(tableName).capitalize(),
    ADAPTER_NAME,
  )
  return PropertySpec.builder(adapterName, adapterType, KModifier.PRIVATE)
    .initializer(adapterName)
    .build()
}

private fun LazyQuery.columns() = when (val parentRule = tableName.parent) {
  is SqlCreateTableStmt -> parentRule.columnDefList.filterNot { it.columnName.isExcludedFromCodegen() }
  else ->
    query.columns
      .filterCodegenExcludedColumns { it.element as? NamedElement }
      .map { (it.element as NamedElement).columnDefSource()!! }
}

internal val LazyQuery.adapterName
  get() = "${SqlDelightCompiler.allocateName(tableName)}$ADAPTER_NAME"
