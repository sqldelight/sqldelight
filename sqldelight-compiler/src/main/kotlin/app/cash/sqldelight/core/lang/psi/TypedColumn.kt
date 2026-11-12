/*
 * Copyright (C) 2017 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.sqldelight.core.lang.psi

import app.cash.sqldelight.core.lang.util.type
import app.cash.sqldelight.dialect.api.IntermediateType
import com.alecstrong.sql.psi.core.psi.SqlColumnExpr
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.intellij.psi.PsiNamedElement
import com.squareup.kotlinpoet.PropertySpec

internal interface TypedColumn {
  /**
   * @return the java type for this column.
   */
  fun type(): IntermediateType

  /**
   * @return the adapter property which will include the type and name of the adapter. If this
   * column does not require an adapter, returns null.
   */
  fun adapter(): PropertySpec?
}

/**
 * @return true if both column names refer to the same column of the same table instance in the
 * query. Two references to the same column definition through different table instances e.g a self
 * join or a view joined to the table it selects from are not the same column instance.
 */
fun SqlColumnName.isColumnSameAs(other: SqlColumnName): Boolean {
  val column = type().column ?: return false
  if (column != other.type().column) return false
  return sourceTable() == other.sourceTable()
}

/**
 * The table, alias or view this column name is selected from or null if it cannot be determined.
 */
private fun SqlColumnName.sourceTable(): PsiNamedElement? {
  val resolved = reference?.resolve() ?: return null
  val tableName = (parent as? SqlColumnExpr)?.tableName
  return queryAvailable(this)
    .filter { tableName == null || it.table?.name == tableName.name }
    .firstOrNull { result -> result.columns.any { it.element == resolved } }
    ?.table
}

fun SqlColumnName.isTypeSameAs(other: SqlColumnName) = type().asNonNullable().javaType == other.type().asNonNullable().javaType
