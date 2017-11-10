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
import com.alecstrong.sqlite.psi.core.psi.SqliteCompoundSelectStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteExpr
import com.intellij.psi.PsiElement
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.sqldelight.core.lang.SqlDelightFile.LabeledStatement
import com.squareup.sqldelight.core.lang.util.sqFile
import com.squareup.sqldelight.core.lang.util.type
import java.util.LinkedHashSet

data class NamedQuery(val name: String, val select: SqliteCompoundSelectStmt) {
  /**
   * Explodes the sqlite query into an ordered list (same order as the query) of abstract methods with
   * names and types corresponding to the query.
   */
  internal val resultColumns: List<NamedColumn> by lazy {
    val namesUsed = LinkedHashSet<String>()

    return@lazy select.queryExposed().flatMap {
      val table = it.table
      return@flatMap it.columns.map {
        var name = it.functionName()
        if (!namesUsed.add(name)) {
          if (table != null) name = "${table}_$name"
          while (!namesUsed.add(name)) name += "_"
        }

        return@map NamedColumn(name, it.type(true))
      }
    }
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

  private fun PsiElement.functionName() = when (this) {
    is NamedElement -> name
    is SqliteExpr -> name
    else -> throw IllegalStateException("Cannot get name for type ${this.javaClass}")
  }
}

data class NamedColumn(val name: String, val type: TypeName)

internal fun Collection<LabeledStatement>.namedQueries(): List<NamedQuery> {
  return filter { it.statement.compoundSelectStmt != null && it.identifier.name != null }
      .map { NamedQuery(it.identifier.name!!, it.statement.compoundSelectStmt!!) }
}

