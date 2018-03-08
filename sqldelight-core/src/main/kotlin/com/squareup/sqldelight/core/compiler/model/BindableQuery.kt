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
package com.squareup.sqldelight.core.compiler.model

import com.alecstrong.sqlite.psi.core.psi.SqliteBindExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteCreateTableStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteTypes
import com.intellij.psi.PsiElement
import com.squareup.sqldelight.core.lang.IntermediateType
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.ARGUMENT
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.NULL
import com.squareup.sqldelight.core.lang.psi.InsertStmtMixin
import com.squareup.sqldelight.core.lang.util.argumentType
import com.squareup.sqldelight.core.lang.util.columns
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.core.lang.util.interfaceType
import com.squareup.sqldelight.core.lang.util.table

open class BindableQuery(
  internal val statement: PsiElement
) {
  /**
   * The collection of parameters exposed in the generated api for this query.
   */
  internal val parameters: List<IntermediateType> by lazy {
    if (statement is InsertStmtMixin && statement.acceptsTableInterface()) {
      val table = statement.table.tableName.parent as SqliteCreateTableStmt
      return@lazy listOf(IntermediateType(
          ARGUMENT,
          table.interfaceType,
          name = table.tableName.name
      ))
    }
    return@lazy arguments.map { it.second }
  }

  /**
   * The collection of all bind expressions in this query.
   */
  internal val arguments: List<Pair<Int, IntermediateType>> by lazy {
    if (statement is InsertStmtMixin && statement.acceptsTableInterface()) {
      return@lazy statement.columns.mapIndexed { index, column -> (index + 1) to column.type() }
    }

    val result = mutableListOf<Pair<Int, IntermediateType>>()
    val indexesSeen = mutableSetOf<Int>()
    val manuallyNamedIndexes = mutableSetOf<Int>()
    val namesSeen = mutableSetOf<String>()
    var maxIndexSeen = 0
    statement.findChildrenOfType<SqliteBindExpr>().forEach { bindArg ->
      bindArg.bindParameter.node.findChildByType(SqliteTypes.DIGIT)?.text?.toInt()?.let { index ->
        if (!indexesSeen.add(index)) {
          val current = result.first { it.first == index }
          if (current.second.sqliteType == NULL) {
            result.remove(current)
            result.add(index to bindArg.argumentType().run { copy(javaType = javaType.asNullable()) })
          }
          return@forEach
        }
        maxIndexSeen = maxOf(maxIndexSeen, index)
        result.add(index to bindArg.argumentType())
        return@forEach
      }
      bindArg.bindParameter.identifier?.let {
        if (!namesSeen.add(it.text)) {
          val current = result.first { (_, type) -> type.name == it.text }
          if (current.second.sqliteType == NULL) {
            result.remove(current)
            result.add(current.first to bindArg.argumentType().run { copy(javaType = javaType.asNullable()) })
          }
          return@forEach
        }
        val index = ++maxIndexSeen
        indexesSeen.add(index)
        manuallyNamedIndexes.add(index)
        result.add(index to bindArg.argumentType().copy(name = it.text))
        return@forEach
      }
      val index = ++maxIndexSeen
      indexesSeen.add(index)
      result.add((index) to bindArg.argumentType())
    }

    // If there are still naming conflicts (edge case where the name we generate is the same as
    // the name a user specified for a different parameter), resolve those.
    result.replaceAll { (index, arg) ->
      var name = arg.name
      while (index !in manuallyNamedIndexes && !namesSeen.add(name)) {
        name += "_"
      }
      index to arg.copy(name = name)
    }

    if (statement is InsertStmtMixin) {
      return@lazy result.map { (index, argument) ->
        val isPrimaryKey = argument.column?.columnConstraintList
            ?.any { it.node?.findChildByType(SqliteTypes.PRIMARY) != null } == true
        if (isPrimaryKey && argument.column?.typeName?.text == "INTEGER") {
          // INTEGER Primary keys can be inserted as null to be auto-assigned a primary key.
          return@map index to argument.copy(javaType = argument.javaType.asNullable())
        }
        return@map index to argument
      }
    }

    return@lazy result
  }
}