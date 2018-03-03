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
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.INNER
import com.squareup.kotlinpoet.KModifier.OUT
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.joinToCode
import com.squareup.sqldelight.core.compiler.model.NamedQuery
import com.squareup.sqldelight.core.lang.DATABASE_NAME
import com.squareup.sqldelight.core.lang.DIRTIED_FUNCTION
import com.squareup.sqldelight.core.lang.MAPPER_NAME
import com.squareup.sqldelight.core.lang.QUERY_TYPE
import com.squareup.sqldelight.core.lang.RESULT_SET_NAME
import com.squareup.sqldelight.core.lang.RESULT_SET_TYPE
import com.squareup.sqldelight.core.lang.STATEMENT_NAME
import com.squareup.sqldelight.core.lang.STATEMENT_TYPE

class SelectQueryGenerator(val query: NamedQuery) {
  /**
   * The exposed query method which returns the default data class implementation.
   *
   * `fun selectForId(id: Int): Query<Data>`
   */
  fun defaultResultTypeFunction(): FunSpec {
    val function = FunSpec.builder(query.name)
    val params = mutableListOf<CodeBlock>()
    query.arguments.forEach { argument ->
      function.addParameter(argument.name, argument.javaType)
      params.add(CodeBlock.of(argument.name))
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
    val function = FunSpec.builder(query.name)
    val params = mutableListOf<CodeBlock>()

    // Adds the actual SqlPreparedStatement:
    // statement = database.getConnection().prepareStatement("SELECT * FROM test")
    // TODO: Handle modifying the select for set parameters (WHERE column IN ?)
    function.addStatement(
        "val $STATEMENT_NAME = $DATABASE_NAME.getConnection().prepareStatement(%S)",
        query.select.text
    )

    // For each parameter in the sql
    query.arguments.forEachIndexed { index, argument ->
      // Adds each sqlite parameter to the argument list:
      // fun <T> selectForId(<<id>>, <<other_param>>, ...)
      function.addParameter(argument.name, argument.javaType)
      params.add(CodeBlock.of(argument.name))

      // Binds each parameter to the statement:
      // statement.bindLong(0, id)
      function.addCode(argument.preparedStatementBinder(index))
    }

    // Assemble the actual mapper lambda:
    // { resultSet ->
    //   mapper(
    //       resultSet.getLong(0),
    //       queryWrapper.tableAdapter.columnAdapter.decode(resultSet.getString(0))
    //   )
    // }
    val mapperLamda = CodeBlock.builder().addStatement(" { $RESULT_SET_NAME ->").indent()

    if (query.resultColumns.size > 1) {
      // Function takes a custom mapper.

      // Add the type variable to the signature.
      val typeVariable = TypeVariableName("T")
      function.addTypeVariable(typeVariable)

      // Add the custom mapper to the signature:
      // mapper: (id: kotlin.Long, value: kotlin.String) -> T
      function.addParameter(ParameterSpec.builder(MAPPER_NAME, LambdaTypeName.get(
          parameters = query.resultColumns.map {
            ParameterSpec.builder(it.name, it.javaType).build()
          },
          returnType = typeVariable
      )).build())

      // Specify the return type for the mapper:
      // Query<T>
      function.returns(ParameterizedTypeName.get(QUERY_TYPE, typeVariable))

      // Add the call of mapper with the deserialized columns:
      // mapper(
      //     resultSet.getLong(0),
      //     queryWrapper.tableAdapter.columnAdapter.decode(resultSet.getString(0))
      // )
      mapperLamda.add("$MAPPER_NAME(\n")
          .indent()
          .apply {
            val decoders = query.resultColumns.mapIndexed { index, column -> column.resultSetGetter(index) }
            add(decoders.joinToCode(separator = ",\n", suffix = "\n"))
          }
          .unindent()
          .add(")\n")
    } else {
      // No custom type possible, just returns the single column:
      // fun selectSomeText(_id): Query<String>
      function.returns(
          ParameterizedTypeName.get(QUERY_TYPE, query.resultColumns.single().javaType))
      mapperLamda.add(query.resultColumns.single().resultSetGetter(0)).add("\n")
    }
    mapperLamda.unindent().add("}\n")

    if (query.arguments.isEmpty()) {
      // No need for a custom query type, return an instance of Query:
      // return Query(statement, selectForId) { resultSet -> ... }
      return function
          .addCode("return %T($STATEMENT_NAME, ${query.name})%L", QUERY_TYPE, mapperLamda.build())
          .build()
    } else {
      // Custom type is needed to handle dirtying events, return an instance of custom type:
      // return SelectForId(id, statement) { resultSet -> ... }
      return function
          .addCode("return ${query.name.capitalize()}(")
          .apply {
            query.arguments.forEach { addCode("${it.name}, ") }
          }
          .addCode("statement)%L", mapperLamda.build())
          .build()
    }
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
   * private class SelectForIdQuery<out T>(
   *   private val _id: Int,
   *   statement: SqlPreparedStatement,
   *   mapper: (SqlResultSet) -> T
   * ) : Query<T>(statement, selectForId, mapper) {
   * private inner class SelectForIdQuery<out T>(
   *   private val _id: Int, mapper: (Cursor) -> T
   * ): Query<T>(database.helper, selectForIdQueries, mapper)
   * ```
   */
  fun querySubtype(): TypeSpec {
    val queryType = TypeSpec.classBuilder(query.name.capitalize())
        .addModifiers(PRIVATE, INNER)

    val constructor = FunSpec.constructorBuilder()

    // The custom return type variable:
    // <out T>
    val returnType = TypeVariableName("T", OUT)
    queryType.addTypeVariable(returnType)

    // The superclass:
    // Query<T>
    queryType.superclass(ParameterizedTypeName.get(QUERY_TYPE, returnType))

    // The dirtied function:
    val dirtiedFunction = FunSpec.builder(DIRTIED_FUNCTION)
        .returns(BOOLEAN)

    // TODO: A bunch of magic to figure out if this select query is dirtied by a mutator query.
    dirtiedFunction.addStatement("return true")

    // For each bind argument the query has.
    query.arguments.forEach {
      // Add the argument as a constructor property. (Used later to figure out if query dirtied)
      // private val id: Int
      queryType.addProperty(PropertySpec.builder(it.name, it.javaType, PRIVATE)
          .initializer(it.name)
          .build())
      constructor.addParameter(it.name, it.javaType)

      // Add the argument as a dirtied function parameter.
      dirtiedFunction.addParameter(it.name, it.javaType)
    }

    // Add the statement as a constructor parameter and pass to the super constructor:
    // statement: SqlPreparedStatement
    constructor.addParameter(STATEMENT_NAME, STATEMENT_TYPE)
    queryType.addSuperclassConstructorParameter(STATEMENT_NAME)

    // Add the query property to the super constructor
    queryType.addSuperclassConstructorParameter(query.name)

    // Add the mapper constructor parameter and pass to the super constructor
    constructor.addParameter(MAPPER_NAME, LambdaTypeName.get(
        parameters = RESULT_SET_TYPE,
        returnType = returnType
    ))
    queryType.addSuperclassConstructorParameter(MAPPER_NAME)

    return queryType
        .primaryConstructor(constructor.build())
        .addFunction(dirtiedFunction.build())
        .build()
  }

  companion object {
    val MUTABLE_LIST_TYPE = ClassName("kotlin.collections", "MutableList")
  }
}
