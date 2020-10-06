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
package com.squareup.sqldelight.core.compiler.model

import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.SqlCompoundSelectStmt
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlTableName
import com.alecstrong.sql.psi.core.psi.SqlValuesExpression
import com.intellij.psi.PsiElement
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler.allocateName
import com.squareup.sqldelight.core.dialect.sqlite.SqliteType.ARGUMENT
import com.squareup.sqldelight.core.dialect.sqlite.SqliteType.BLOB
import com.squareup.sqldelight.core.dialect.sqlite.SqliteType.INTEGER
import com.squareup.sqldelight.core.dialect.sqlite.SqliteType.NULL
import com.squareup.sqldelight.core.dialect.sqlite.SqliteType.REAL
import com.squareup.sqldelight.core.dialect.sqlite.SqliteType.TEXT
import com.squareup.sqldelight.core.lang.CUSTOM_DATABASE_NAME
import com.squareup.sqldelight.core.lang.IntermediateType
import com.squareup.sqldelight.core.lang.queriesName
import com.squareup.sqldelight.core.lang.util.name
import com.squareup.sqldelight.core.lang.util.sqFile
import com.squareup.sqldelight.core.lang.util.tablesObserved
import com.squareup.sqldelight.core.lang.util.type

data class NamedQuery(
  val name: String,
  val select: SqlCompoundSelectStmt,
  private val statementIdentifier: PsiElement? = null
) : BindableQuery(statementIdentifier, select) {
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
          val table = it.table?.name
          return@flatMap it.columns.map { (element, nullable, compounded) ->
            var name = element.functionName()
            if (!namesUsed.add(name)) {
              if (table != null) name = "${table}_$name"
              while (!namesUsed.add(name)) name += "_"
            }

            return@map compounded.fold(element.type()) { type, column ->
              superType(type, column.element.type().nullableIf(column.nullable))
            }.let {
              it.copy(
                  name = name,
                  javaType = if (nullable) it.javaType.copy(nullable = true) else it.javaType
              )
            }
          }
        }
      }
      if (results.isEmpty()) {
        return@fold compoundSelect
      }
      return@fold results.zip(compoundSelect, this::superType)
    })
  }

  /**
   * If this query is a pure select from a table (virtual or otherwise), this returns the LazyQuery
   * which points to that table (Pure meaning it has exactly the same columns in the same order).
   */
  private val pureTable: LazyQuery? by lazy {
    return@lazy select.tablesAvailable(select).firstOrNull {
      it.query.columns == select.queryExposed().singleOrNull()?.columns
    }
  }

  /**
   * The name of the generated interface that this query references. The linked interface will have
   * a default implementation subclass.
   */
  internal val interfaceType: ClassName by lazy {
    pureTable?.let {
      return@lazy ClassName(it.tableName.sqFile().packageName!!, allocateName(it.tableName).capitalize())
    }
    return@lazy ClassName(select.sqFile().packageName!!, name.capitalize())
  }

  /**
   * @return true if this query needs its own interface generated.
   */
  internal fun needsInterface() = needsWrapper() && pureTable == null

  internal fun needsWrapper() = (resultColumns.size > 1 || resultColumns[0].javaType.isNullable)

  internal val tablesObserved: List<SqlTableName> by lazy { select.tablesObserved() }

  internal val queryProperty =
      CodeBlock.of("$CUSTOM_DATABASE_NAME.${select.sqFile().queriesName}.$name")

  internal val customQuerySubtype = "${name.capitalize()}Query"

  private fun resultColumns(valuesList: List<SqlValuesExpression>): List<IntermediateType> {
    return valuesList.fold(emptyList(), { results, values ->
      val exposedTypes = values.exprList.map { it.type() }
      if (results.isEmpty()) return@fold exposedTypes
      return@fold results.zip(exposedTypes, this::superType)
    })
  }

  private fun superType(typeOne: IntermediateType, typeTwo: IntermediateType): IntermediateType {
    // Arguments types always take the other type.
    if (typeOne.dialectType == ARGUMENT) {
      return typeTwo.copy(name = typeOne.name)
    } else if (typeTwo.dialectType == ARGUMENT) {
      return typeOne
    }

    // Nullable types take nullable version of the other type.
    if (typeOne.dialectType == NULL) {
      return typeTwo.asNullable().copy(name = typeOne.name)
    } else if (typeTwo.dialectType == NULL) {
      return typeOne.asNullable()
    }

    val nullable = typeOne.javaType.isNullable || typeTwo.javaType.isNullable

    if (typeOne.dialectType != typeTwo.dialectType) {
      // Incompatible sqlite types. Prefer the type which can contain the other.
      // NULL < INTEGER < REAL < TEXT < BLOB
      val type = listOf(NULL, INTEGER, REAL, TEXT, BLOB)
          .last { it == typeOne.dialectType || it == typeTwo.dialectType }
      return IntermediateType(dialectType = type, name = typeOne.name).nullableIf(nullable)
    }

    if (typeOne.column !== typeTwo.column &&
        typeOne.cursorGetter(0) != typeTwo.cursorGetter(0) &&
        typeOne.column != null && typeTwo.column != null) {
      // Incompatible adapters. Revert to unadapted java type.
      return IntermediateType(dialectType = typeOne.dialectType, name = typeOne.name).nullableIf(nullable)
    }

    return typeOne.nullableIf(nullable)
  }

  private fun PsiElement.functionName() = when (this) {
    is NamedElement -> allocateName(this)
    is SqlExpr -> name
    else -> throw IllegalStateException("Cannot get name for type ${this.javaClass}")
  }

  override val id: Int
    // the sqlFile package name -> com.example.
    // sqlFile.name -> test.sq
    // name -> query name
    get() = getUniqueQueryIdentifier(statement.sqFile().let { "${it.packageName}:${it.name}:$name" })
}
