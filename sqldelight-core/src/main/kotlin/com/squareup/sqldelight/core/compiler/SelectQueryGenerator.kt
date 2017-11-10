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

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.joinToCode

class SelectQueryGenerator(val query: NamedQuery) {
  /**
   * The exposed query method which returns the default data class implementation.
   *
   * `fun selectForId(id: Int): Query<Data>`
   */
  fun defaultResultTypeFunction(): FunSpec {
    val function = FunSpec.builder(query.name)
    val params = mutableListOf<CodeBlock>()
    query.resultColumns.forEach { (name, type) ->
      function.addParameter(name, type)
      params.add(CodeBlock.of(name))
    }
    params.add(CodeBlock.of("%T::Impl", query.interfaceType))
    return function
        .returns(ParameterizedTypeName.get(QUERY_TYPE, query.interfaceType))
        .addCode(params.joinToCode(", ", "return ${query.name}(", ")"))
        .build()
  }

  /**
   * The exposed query method which returns a provided custom type.
   *
   * `fun <T> selectForId(id, mapper: (column1: String) -> T): Query<T>`
   */
  fun customResultTypeFunction(): FunSpec {
    TODO("")
  }

  /**
   * The private property used to delegate query result updates.
   *
   * `private val selectForIdQueries = mutableListOf<Query<*>>()`
   */
  fun queryCollectionProperty(): PropertySpec {
    val queryType = ParameterizedTypeName.get(QUERY_TYPE, WildcardTypeName.subtypeOf(ANY))
    val listType = ParameterizedTypeName.get(MUTABLE_LIST_TYPE, queryType)
    return PropertySpec.builder("${query.name}Queries", listType, PRIVATE)
        .initializer("mutableListOf<>()")
        .build()
  }

  /**
   * The private query subtype for this specific query.
   *
   * ```
   * private inner class SelectForIdQuery<out T>(
   *   private val _id: Int, mapper: (Cursor) -> T
   * ): Query<T>(database.helper, selectForIdQueries, mapper)
   * ```
   */
  fun querySubtype(): TypeSpec {
    TODO("")
  }

  companion object {
    val MUTABLE_LIST_TYPE = ClassName("kotlin.collections", "MutableList")
    val QUERY_TYPE = ClassName("com.squareup.sqldelight", "Query")
  }
}
