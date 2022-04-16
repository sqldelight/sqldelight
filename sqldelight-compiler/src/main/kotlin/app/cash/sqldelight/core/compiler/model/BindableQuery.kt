/*
 * Copyright (C) 2018 Square, Inc.
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

import app.cash.sqldelight.core.compiler.SqlDelightCompiler.allocateName
import app.cash.sqldelight.core.lang.acceptsTableInterface
import app.cash.sqldelight.core.lang.psi.ColumnTypeMixin.ValueTypeDialectType
import app.cash.sqldelight.core.lang.psi.StmtIdentifierMixin
import app.cash.sqldelight.core.lang.types.typeResolver
import app.cash.sqldelight.core.lang.util.argumentType
import app.cash.sqldelight.core.lang.util.childOfType
import app.cash.sqldelight.core.lang.util.columns
import app.cash.sqldelight.core.lang.util.findChildrenOfType
import app.cash.sqldelight.core.lang.util.sqFile
import app.cash.sqldelight.core.lang.util.type
import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType.ARGUMENT
import app.cash.sqldelight.dialect.api.PrimitiveType.BOOLEAN
import app.cash.sqldelight.dialect.api.PrimitiveType.INTEGER
import app.cash.sqldelight.dialect.api.PrimitiveType.NULL
import com.alecstrong.sql.psi.core.psi.SqlAnnotatedElement
import com.alecstrong.sql.psi.core.psi.SqlBindExpr
import com.alecstrong.sql.psi.core.psi.SqlBindParameter
import com.alecstrong.sql.psi.core.psi.SqlIdentifier
import com.alecstrong.sql.psi.core.psi.SqlInsertStmt
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.psi.PsiElement
import com.squareup.kotlinpoet.ClassName
import java.util.concurrent.ConcurrentHashMap

abstract class BindableQuery(
  internal val identifier: StmtIdentifierMixin?,
  internal val statement: SqlAnnotatedElement
) {
  protected val typeResolver = statement.typeResolver

  abstract val id: Int

  internal val javadoc: PsiElement? = identifier?.childOfType(SqlTypes.JAVADOC)

  /**
   * The collection of parameters exposed in the generated api for this query.
   */
  internal val parameters: List<IntermediateType> by lazy {
    if (statement is SqlInsertStmt && statement.acceptsTableInterface()) {
      val table = statement.tableName.reference!!.resolve()!!
      return@lazy listOf(
        IntermediateType(
          ARGUMENT,
          javaType = ClassName(table.sqFile().packageName!!, allocateName(statement.tableName).capitalize()),
          name = allocateName(statement.tableName)
        )
      )
    }
    return@lazy arguments.sortedBy { it.index }.map { it.type }
  }

  /**
   * The collection of all bind expressions in this query.
   */
  internal val arguments: List<Argument> by lazy {
    if (statement is SqlInsertStmt && statement.acceptsTableInterface()) {
      return@lazy statement.columns.mapIndexed { index, column ->
        Argument(
          index + 1,
          column.type().let {
            it.copy(
              name = "${allocateName(statement.tableName)}.${it.name}"
            )
          }
        )
      }
    }

    val result = mutableListOf<Argument>()
    val indexesSeen = mutableSetOf<Int>()
    val manuallyNamedIndexes = mutableSetOf<Int>()
    val namesSeen = mutableSetOf<String>()
    var maxIndexSeen = 0
    statement.findChildrenOfType<SqlBindExpr>().forEach { bindArg ->
      bindArg.bindParameter.node.findChildByType(SqlTypes.DIGIT)?.text?.toInt()?.let { index ->
        if (!indexesSeen.add(index)) {
          result.findAndReplace(bindArg, index) { it.index == index }
          return@forEach
        }
        maxIndexSeen = maxOf(maxIndexSeen, index)
        result.add(Argument(index, typeResolver.argumentType(bindArg), mutableListOf(bindArg)))
        return@forEach
      }
      bindArg.bindParameter.identifier?.let {
        if (!namesSeen.add(it.text)) {
          result.findAndReplace(bindArg) { (_, type, _) -> type.name == it.text }
          return@forEach
        }
        val index = ++maxIndexSeen
        indexesSeen.add(index)
        manuallyNamedIndexes.add(index)
        result.add(Argument(index, typeResolver.argumentType(bindArg).copy(name = it.text), mutableListOf(bindArg)))
        return@forEach
      }
      val index = ++maxIndexSeen
      indexesSeen.add(index)
      result.add(Argument(index, typeResolver.argumentType(bindArg), mutableListOf(bindArg)))
    }

    // If there are still naming conflicts (edge case where the name we generate is the same as
    // the name a user specified for a different parameter), resolve those.
    result.replaceAll {
      var name = it.type.name
      while (it.index !in manuallyNamedIndexes && !namesSeen.add(name)) {
        name += "_"
      }
      it.copy(type = it.type.copy(name = name))
    }

    if (statement is SqlInsertStmt) {
      return@lazy result.map {
        val isPrimaryKey = it.type.column?.columnConstraintList
          ?.any { it.node?.findChildByType(SqlTypes.PRIMARY) != null } == true
        if (isPrimaryKey && it.type.column?.columnType?.typeName?.text == "INTEGER") {
          // INTEGER Primary keys can be inserted as null to be auto-assigned a primary key.
          return@map it.copy(type = it.type.asNullable())
        }
        return@map it
      }
    }

    return@lazy result
  }

  private fun MutableList<Argument>.findAndReplace(
    bindArg: SqlBindExpr,
    index: Int? = null,
    condition: (Argument) -> Boolean
  ) {
    val current = first(condition)
    current.bindArgs.add(bindArg)
    val newType = typeResolver.argumentType(bindArg)

    val newArgumentType = when {
      // If we currently have a NULL type for this argument but encounter a different type later,
      // then the new type must be nullable.
      // i.e. WHERE (:foo IS NULL OR data = :foo)
      current.type.dialectType == NULL -> newType
      current.type.dialectType == INTEGER && newType.dialectType == BOOLEAN -> newType
      // If we'd previously assigned a type to this argument other than NULL, and later encounter NULL,
      // we should update the existing type to be nullable.
      // i.e. WHERE (data = :foo OR :foo IS NULL)
      newType.dialectType == NULL && current.type.dialectType != NULL -> current.type
      // If the new type is just a wrapped type, use it.
      newType.dialectType is ValueTypeDialectType -> newType
      // Nothing to update
      else -> null
    }

    if (newArgumentType != null) {
      remove(current)
      add(
        current.copy(
          index = index ?: current.index,
          type = newArgumentType.run {
            copy(
              javaType = javaType.copy(nullable = current.type.javaType.isNullable || newType.javaType.isNullable),
              name = bindArg.bindParameter.identifier?.text ?: name
            )
          }
        )
      )
    }
  }

  internal fun idForIndex(index: Int?): Int {
    val postFix = if (index == null) "" else "_$index"
    return getUniqueQueryIdentifier(
      statement.sqFile().let {
        "${it.packageName}:${it.name}:${identifier?.name ?: ""}$postFix"
      }
    )
  }

  private val SqlBindParameter.identifier: SqlIdentifier?
    get() = childOfType(SqlTypes.IDENTIFIER) as? SqlIdentifier

  internal data class Argument(
    val index: Int,
    val type: IntermediateType,
    val bindArgs: MutableList<SqlBindExpr> = mutableListOf()
  )

  companion object {
    /**
     * The query id map use to avoid string hashcode collision. Ideally this map should be per module.
     */
    val queryIdMap = ConcurrentHashMap<String, Int>()

    /**
     * Use the hashcode of qualifiedQueryName to generate the unique identifier id for queries. Detect the
     * hashcode collision by caching the generated identifiers. Runtime exception will be thrown when collision happens.
     * Client would need to give a different query name to avoid the collision.
     */
    fun getUniqueQueryIdentifier(qualifiedQueryName: String): Int {
      return when (queryIdMap.containsKey(qualifiedQueryName)) {
        true -> queryIdMap[qualifiedQueryName]!!
        else -> {
          val queryId = qualifiedQueryName.hashCode()
          if (queryIdMap.values.contains(queryId)) {
            // throw an exception here to ask the client to give a different query name which will not cause hashcode collision.
            // this should not happen often, when it happens, should be an easy fix for the client
            // to give a different query than adding logic to generate deterministic identifier
            throw RuntimeException(
              "HashCode collision happened when generating unique identifier for $qualifiedQueryName." +
                "Please give a different name"
            )
          }
          queryIdMap[qualifiedQueryName] = queryId
          queryId
        }
      }
    }
  }
}
