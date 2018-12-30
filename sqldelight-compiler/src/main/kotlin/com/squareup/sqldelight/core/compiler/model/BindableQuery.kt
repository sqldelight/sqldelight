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
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler.allocateName
import com.squareup.sqldelight.core.lang.IntermediateType
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.ARGUMENT
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.NULL
import com.squareup.sqldelight.core.lang.psi.InsertStmtMixin
import com.squareup.sqldelight.core.lang.util.argumentType
import com.squareup.sqldelight.core.lang.util.childOfType
import com.squareup.sqldelight.core.lang.util.columns
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.core.lang.util.interfaceType
import com.squareup.sqldelight.core.lang.util.table

abstract class BindableQuery(
  internal val identifier: PsiElement?,
  internal val statement: PsiElement
) {
  val id = currentId++

  private val javadoc: PsiElement? = identifier?.childOfType(SqliteTypes.JAVADOC)

  /**
   * The collection of parameters exposed in the generated api for this query.
   */
  internal val parameters: List<IntermediateType> by lazy {
    if (statement is InsertStmtMixin && statement.acceptsTableInterface()) {
      val table = statement.table.tableName.parent as SqliteCreateTableStmt
      return@lazy listOf(IntermediateType(
          ARGUMENT,
          table.interfaceType,
          name = allocateName(table.tableName)
      ))
    }
    return@lazy arguments.map { it.type }
  }

  /**
   * The collection of all bind expressions in this query.
   */
  internal val arguments: List<Argument> by lazy {
    if (statement is InsertStmtMixin && statement.acceptsTableInterface()) {
      return@lazy statement.columns.mapIndexed { index, column ->
        Argument(index + 1, column.type().let {
          it.copy(
              name = "${statement.tableName.name}.${it.name}",
              extracted = true
          )
        })
      }
    }

    val result = mutableListOf<Argument>()
    val indexesSeen = mutableSetOf<Int>()
    val manuallyNamedIndexes = mutableSetOf<Int>()
    val namesSeen = mutableSetOf<String>()
    var maxIndexSeen = 0
    statement.findChildrenOfType<SqliteBindExpr>().forEach { bindArg ->
      bindArg.bindParameter.node.findChildByType(SqliteTypes.DIGIT)?.text?.toInt()?.let { index ->
        if (!indexesSeen.add(index)) {
          result.findAndReplace(bindArg, index) { it.index == index }
          return@forEach
        }
        maxIndexSeen = maxOf(maxIndexSeen, index)
        result.add(Argument(index, bindArg.argumentType(), mutableListOf(bindArg)))
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
        result.add(Argument(index, bindArg.argumentType().copy(name = it.text), mutableListOf(bindArg)))
        return@forEach
      }
      val index = ++maxIndexSeen
      indexesSeen.add(index)
      result.add(Argument(index, bindArg.argumentType(), mutableListOf(bindArg)))
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

    if (statement is InsertStmtMixin) {
      return@lazy result.map {
        val isPrimaryKey = it.type.column?.columnConstraintList
            ?.any { it.node?.findChildByType(SqliteTypes.PRIMARY) != null } == true
        if (isPrimaryKey && it.type.column?.typeName?.text == "INTEGER") {
          // INTEGER Primary keys can be inserted as null to be auto-assigned a primary key.
          return@map it.copy(type = it.type.copy(javaType = it.type.javaType.copy(nullable = true)))
        }
        return@map it
      }
    }

    return@lazy result
  }

  private fun MutableList<Argument>.findAndReplace(
    bindArg: SqliteBindExpr,
    index: Int? = null,
    condition: (Argument) -> Boolean
  ) {
    val current = first(condition)
    current.bindArgs.add(bindArg)
    if (current.type.sqliteType == NULL) {
      remove(current)
      add(current.copy(
          index = index ?: current.index,
          type = bindArg.argumentType().run {
            copy(
                javaType = javaType.copy(nullable = true),
                name = bindArg.bindParameter.identifier?.text ?: name
            )
          }
      ))
    }
  }

  internal fun javadocText(): String? {
    if (javadoc == null) return null
    return javadoc.text
        .split(JAVADOC_TEXT_REGEX)
        .dropWhile(String::isEmpty)
        .joinToString(separator = "\n", transform = String::trim)
  }

  internal abstract fun type(): CodeBlock

  internal data class Argument(
    val index: Int,
    val type: IntermediateType,
    val bindArgs: MutableList<SqliteBindExpr> = mutableListOf()
  )

  companion object {
    /**
     * This pattern consists of 3 parts:
     *
     * - `/\\*\\*` - matches the first line of the Javadoc:
     *
     * ```
     * </**>
     *  * Javadoc
     *  */
     * ```
     *
     * - `\n \\*[ /]?` - matches every other line of Javadoc:
     *
     * ```
     * /**<
     *  * >Javadoc<
     *  */>
     * ```
     *
     * - ` \\**slash` - specifically matches the tail part of a single-line Javadoc:
     *
     * ```
     * /* Javadoc< */>
     * ```
     */
    private val JAVADOC_TEXT_REGEX = Regex("/\\*\\*|\n \\*[ /]?| \\*/")

    private var currentId = 0
  }
}