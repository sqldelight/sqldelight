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
package app.cash.sqldelight.core.compiler.model

import app.cash.sqldelight.core.capitalize
import app.cash.sqldelight.core.compiler.SqlDelightCompiler.allocateName
import app.cash.sqldelight.core.decapitalize
import app.cash.sqldelight.core.lang.SqlDelightQueriesFile
import app.cash.sqldelight.core.lang.cursorGetter
import app.cash.sqldelight.core.lang.parentAdapter
import app.cash.sqldelight.core.lang.psi.StmtIdentifierMixin
import app.cash.sqldelight.core.lang.util.TableNameElement
import app.cash.sqldelight.core.lang.util.name
import app.cash.sqldelight.core.lang.util.sqFile
import app.cash.sqldelight.core.lang.util.tablesObserved
import app.cash.sqldelight.core.lang.util.type
import app.cash.sqldelight.core.psi.SqlDelightStmtClojureStmtList
import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType.ARGUMENT
import app.cash.sqldelight.dialect.api.PrimitiveType.BLOB
import app.cash.sqldelight.dialect.api.PrimitiveType.BOOLEAN
import app.cash.sqldelight.dialect.api.PrimitiveType.INTEGER
import app.cash.sqldelight.dialect.api.PrimitiveType.NULL
import app.cash.sqldelight.dialect.api.PrimitiveType.REAL
import app.cash.sqldelight.dialect.api.PrimitiveType.TEXT
import app.cash.sqldelight.dialect.api.QueryWithResults
import app.cash.sqldelight.dialect.api.SelectQueryable
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.SqlCompoundSelectStmt
import com.alecstrong.sql.psi.core.psi.SqlCreateVirtualTableStmt
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlPragmaName
import com.alecstrong.sql.psi.core.psi.SqlValuesExpression
import com.intellij.psi.PsiElement
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.PropertySpec

data class NamedQuery(
  val name: String,
  val queryable: QueryWithResults,
  private val statementIdentifier: StmtIdentifierMixin? = null,
) : BindableQuery(statementIdentifier, queryable.statement) {
  internal val select get() = queryable.statement
  internal val pureTable get() = queryable.pureTable

  /**
   * Explodes the sqlite query into an ordered list (same order as the query) of types to be exposed
   * by the generated api.
   */
  val resultColumns: List<IntermediateType> by lazy {
    if (queryable is SelectQueryable) {
      resultColumns(queryable.select)
    } else {
      queryable.select.typesExposed(LinkedHashSet())
    }
  }

  private fun resultColumns(select: SqlCompoundSelectStmt): List<IntermediateType> {
    val namesUsed = LinkedHashSet<String>()

    return select.selectStmtList.fold(emptyList()) { results, select ->
      val compoundSelect = if (select.valuesExpressionList.isNotEmpty()) {
        resultColumns(select.valuesExpressionList)
      } else {
        select.typesExposed(namesUsed)
      }
      if (results.isEmpty()) {
        return@fold compoundSelect
      }
      return@fold results.zip(compoundSelect, this::superType)
    }
  }

  /**
   * Explodes the sqlite query into an ordered list (same order as the query) of adapters required for
   * the types to be exposed by the generated api.
   */
  internal val resultColumnRequiredAdapters: List<PropertySpec> by lazy {
    if (queryable is SelectQueryable) {
      resultColumnRequiredAdapters(queryable.select)
    } else {
      queryable.select.typesExposed(LinkedHashSet()).mapNotNull { it.parentAdapter() }
    }
  }

  private fun resultColumnRequiredAdapters(select: SqlCompoundSelectStmt): List<PropertySpec> {
    val namesUsed = LinkedHashSet<String>()

    return select.selectStmtList.flatMap { select ->
      if (select.valuesExpressionList.isNotEmpty()) {
        resultColumns(select.valuesExpressionList)
      } else {
        select.typesExposed(namesUsed)
      }.mapNotNull { it.parentAdapter() }
    }
  }

  /**
   * The name of the generated interface that this query references. The linked interface will have
   * a default implementation subclass.
   */
  internal val interfaceType: ClassName by lazy {
    val pureTable = pureTable
    if (pureTable != null && pureTable.parent !is SqlCreateVirtualTableStmt) {
      return@lazy ClassName(pureTable.sqFile().packageName!!, allocateName(pureTable).capitalize())
    }
    var packageName = queryable.select.sqFile().packageName!!
    if (queryable.select.sqFile().parent?.files
        ?.filterIsInstance<SqlDelightQueriesFile>()?.flatMap { it.namedQueries }
        ?.filter { it.needsInterface() && it != this }
        ?.any { it.name == name } == true
    ) {
      packageName = "$packageName.${queryable.select.sqFile().virtualFile!!.nameWithoutExtension.decapitalize()}"
    }
    return@lazy ClassName(packageName, name.capitalize())
  }

  /**
   * @return true if this query needs its own interface generated.
   */
  internal fun needsInterface(): Boolean {
    val needsWrapper = needsWrapper()
    val pureTable = pureTable
    val parent = pureTable?.parent

    return needsWrapper && (pureTable == null || parent is SqlCreateVirtualTableStmt)
  }

  internal fun needsWrapper() = (resultColumns.size > 1 || resultColumns[0].javaType.isNullable)

  internal fun needsQuerySubType() = arguments.isNotEmpty() || statement is SqlDelightStmtClojureStmtList

  internal val tablesObserved: List<TableNameElement>? by lazy {
    if (queryable is SelectQueryable && queryable.select == queryable.statement) {
      queryable.select.tablesObserved()
    } else {
      null
    }
  }

  internal val customQuerySubtype = "${name.capitalize()}Query"

  private fun resultColumns(valuesList: List<SqlValuesExpression>): List<IntermediateType> {
    return valuesList.fold(
      emptyList(),
      { results, values ->
        val exposedTypes = values.exprList.map { it.type() }
        if (results.isEmpty()) return@fold exposedTypes
        return@fold results.zip(exposedTypes, this::superType)
      },
    )
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
      // Incompatible dialect types. Prefer the type which can contain the other.
      // NULL < INTEGER < REAL < TEXT < BLOB
      val type = listOf(NULL, INTEGER, BOOLEAN, REAL, TEXT, BLOB)
        .last { it == typeOne.dialectType || it == typeTwo.dialectType }
      return IntermediateType(dialectType = type, name = typeOne.name).nullableIf(nullable)
    }

    if (typeOne.column !== typeTwo.column &&
      typeOne.asNonNullable().cursorGetter(0) != typeTwo.asNonNullable().cursorGetter(0) &&
      typeOne.column != null &&
      typeTwo.column != null
    ) {
      // Incompatible adapters. Revert to unadapted java type.
      return if (typeOne.javaType.copy(nullable = false) == typeTwo.javaType.copy(nullable = false)) {
        typeOne.copy(assumedCompatibleTypes = typeOne.assumedCompatibleTypes + typeTwo).nullableIf(nullable)
      } else {
        IntermediateType(dialectType = typeOne.dialectType, name = typeOne.name).nullableIf(nullable)
      }
    }

    return typeOne.nullableIf(nullable)
  }

  private fun PsiElement.functionName() = when (this) {
    is NamedElement -> allocateName(this)
    is SqlExpr -> name
    is SqlPragmaName -> text
    else -> throw IllegalStateException("Cannot get name for type ${this.javaClass}")
  }

  private fun QueryElement.typesExposed(
    namesUsed: LinkedHashSet<String>,
  ): List<IntermediateType> {
    return queryExposed().flatMap {
      val table = it.table?.name
      return@flatMap it.columns.map { queryColumn ->
        var name = queryColumn.element.functionName()
        if (!namesUsed.add(name)) {
          if (table != null) name = "${table}_$name"
          while (!namesUsed.add(name)) name += "_"
        }

        return@map queryColumn.type().copy(name = name)
      }
    }
  }

  private fun QueryElement.QueryColumn.type(): IntermediateType {
    var rootType = element.type()
    nullable?.let { rootType = rootType.nullableIf(it) }
    return compounded.fold(rootType) { type, column -> superType(type, column.type()) }
  }

  override val id: Int
    // the sqlFile package name -> com.example.
    // sqlFile.name -> test.sq
    // name -> query name
    get() = getUniqueQueryIdentifier(statement.sqFile().let { "${it.packageName}:${it.name}:$name" })
}
