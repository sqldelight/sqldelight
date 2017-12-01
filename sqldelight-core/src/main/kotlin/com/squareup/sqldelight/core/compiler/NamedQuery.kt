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
package com.squareup.sqldelight.core.compiler

import com.alecstrong.sqlite.psi.core.psi.LazyQuery
import com.alecstrong.sqlite.psi.core.psi.NamedElement
import com.alecstrong.sqlite.psi.core.psi.SqliteBindExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteCompoundSelectStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteValuesExpression
import com.intellij.psi.PsiElement
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.sqldelight.core.lang.IntermediateType
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.ARGUMENT
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.BLOB
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.INTEGER
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.NULL
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.REAL
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.TEXT
import com.squareup.sqldelight.core.lang.SqlDelightFile.LabeledStatement
import com.squareup.sqldelight.core.lang.util.argumentType
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.core.lang.util.name
import com.squareup.sqldelight.core.lang.util.sqFile
import com.squareup.sqldelight.core.lang.util.type
import java.util.LinkedHashSet

data class NamedQuery(val name: String, val select: SqliteCompoundSelectStmt) {
  /**
   * Explodes the sqlite query into an ordered list (same order as the query) of types to be exposed
   * by the generated api.
   */
  internal val resultColumns: List<IntermediateType> by lazy {
    val namesUsed = LinkedHashSet<String>()

    select.selectStmtList.fold(emptyList<IntermediateType>(), { results, select ->
      val compoundSelect: List<IntermediateType>
      if (select.valuesExpressionList.isNotEmpty()) {
        compoundSelect = resultColumns(select.valuesExpressionList)
      } else {
        compoundSelect = select.queryExposed().flatMap {
          val table = it.table
          return@flatMap it.columns.map {
            var name = it.functionName()
            if (!namesUsed.add(name)) {
              if (table != null) name = "${table}_$name"
              while (!namesUsed.add(name)) name += "_"
            }

            return@map it.type().copy(name = name)
          }
        }
      }
      if (results.isEmpty()) return@fold compoundSelect
      return@fold results.zip(compoundSelect, this::superType)
    })
  }

  /**
   * If this query is a pure select from a table (virtual or otherwise), this returns the LazyQuery
   * which points to that table (Pure meaning it has exactly the same columns in the same order).
   */
  private val pureTable: LazyQuery? by lazy {
    return@lazy select.tablesAvailable(select).firstOrNull {
      it.query() == select.queryExposed().singleOrNull()
    }
  }

  /**
   * The name of the generated interface that this query references. The linked interface will have
   * a default implementation subclass.
   */
  internal val interfaceType: TypeName by lazy {
    pureTable?.let {
      return@lazy ClassName(it.tableName.sqFile().packageName, it.tableName.name.capitalize())
    }
    return@lazy ClassName(select.sqFile().packageName, name.capitalize())
  }

  /**
   * @return true if this query needs its own interface generated.
   */
  internal fun needsInterface(): Boolean = resultColumns.size > 1 && pureTable == null

  private fun resultColumns(valuesList: List<SqliteValuesExpression>): List<IntermediateType> {
    return valuesList.fold(emptyList(), { results, values ->
      val exposedTypes = values.exprList.map { it.type() }
      if (results.isEmpty()) return@fold exposedTypes
      return@fold results.zip(exposedTypes, this::superType)
    })
  }

  private fun superType(typeOne: IntermediateType, typeTwo: IntermediateType): IntermediateType {
    // Arguments types always take the other type.
    if (typeOne.sqliteType == ARGUMENT) return typeTwo
    if (typeTwo.sqliteType == ARGUMENT) return typeOne

    val nullable = typeOne.javaType.nullable || typeTwo.javaType.nullable

    if (typeOne.sqliteType != typeTwo.sqliteType) {
      // Incompatible sqlite types. Prefer the type which can contain the other.
      // NULL < INTEGER < REAL < TEXT < BLOB
      val type = listOf(NULL, INTEGER, REAL, TEXT, BLOB)
          .last { it == typeOne.sqliteType || it == typeTwo.sqliteType }
      return IntermediateType(sqliteType = type, name = typeOne.name).nullableIf(nullable)
    }

    if (typeOne.column !== typeTwo.column) {
      // Incompatible adapters. Revert to unadapted java type.
      return IntermediateType(sqliteType = typeOne.sqliteType, name = typeOne.name).nullableIf(nullable)
    }

    return typeOne.nullableIf(nullable)
  }

  private fun PsiElement.functionName() = when (this) {
    is NamedElement -> name
    is SqliteExpr -> name
    else -> throw IllegalStateException("Cannot get name for type ${this.javaClass}")
  }
}

internal fun Collection<LabeledStatement>.namedQueries(): List<NamedQuery> {
  return filter { it.statement.compoundSelectStmt != null && it.identifier.name != null }
      .map { NamedQuery(it.identifier.name!!, it.statement.compoundSelectStmt!!) }
}

