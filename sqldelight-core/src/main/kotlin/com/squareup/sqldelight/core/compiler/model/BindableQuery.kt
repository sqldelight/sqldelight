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
import com.alecstrong.sqlite.psi.core.psi.SqliteTypes
import com.intellij.psi.PsiElement
import com.squareup.sqldelight.core.lang.IntermediateType
import com.squareup.sqldelight.core.lang.util.argumentType
import com.squareup.sqldelight.core.lang.util.findChildrenOfType

open class BindableQuery(
  private val statement: PsiElement
) {
  /**
   * The collection of all bind expressions in this query.
   */
  internal val arguments: List<Pair<Int, IntermediateType>> by lazy {
    val result = mutableListOf<Pair<Int, IntermediateType>>()
    val indexesSeen = mutableSetOf<Int>()
    val manuallyNamedIndexes = mutableSetOf<Int>()
    val namesSeen = mutableSetOf<String>()
    var maxIndexSeen = 0
    statement.findChildrenOfType<SqliteBindExpr>().forEach { bindArg ->
      bindArg.bindParameter.node.findChildByType(SqliteTypes.DIGIT)?.text?.toInt()?.let { index ->
        if (!indexesSeen.add(index)) return@forEach
        maxIndexSeen = maxOf(maxIndexSeen, index)
        result.add(index to bindArg.argumentType())
        return@forEach
      }
      val index = ++maxIndexSeen
      indexesSeen.add(index)
      bindArg.bindParameter.identifier?.let {
        if (!namesSeen.add(it.text)) return@forEach
        manuallyNamedIndexes.add(index)
        result.add(index to bindArg.argumentType().copy(name = it.text))
        return@forEach
      }
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

    return@lazy result
  }
}