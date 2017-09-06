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
package com.squareup.sqldelight.core.lang.util

import com.alecstrong.sqlite.psi.core.psi.NamedElement
import com.alecstrong.sqlite.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sqlite.psi.core.psi.SqliteExpr
import com.intellij.psi.PsiElement
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.ABSTRACT
import java.util.LinkedHashSet

/**
 * Explodes a sqlite query into an ordered list (same order as the query) of abstract methods with
 * names and types corresponding to the query.
 */
internal fun List<QueryResult>.flatFunctions(): List<FunSpec> {
  val namesUsed = LinkedHashSet<String>()

  return flatMap {
    val table = it.table
    return@flatMap it.columns.map {
      var name = it.functionName()
      if (!namesUsed.add(name)) {
        if (table != null) name = "${table}_$name"
        while (!namesUsed.add(name)) name += "_"
      }

      return@map FunSpec.builder(name)
          .addModifiers(ABSTRACT)
          .returns(it.type(true))
          .build()
    }
  }
}

private fun PsiElement.functionName() = when (this) {
  is NamedElement -> name
  is SqliteExpr -> name
  else -> throw IllegalStateException("Cannot get name for type ${this.javaClass}")
}
