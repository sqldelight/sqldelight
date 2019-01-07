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
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.INNER
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.OUT
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.joinToCode
import com.squareup.sqldelight.core.compiler.model.NamedQuery
import com.squareup.sqldelight.core.lang.CURSOR_NAME
import com.squareup.sqldelight.core.lang.CURSOR_TYPE
import com.squareup.sqldelight.core.lang.DRIVER_NAME
import com.squareup.sqldelight.core.lang.EXECUTE_METHOD
import com.squareup.sqldelight.core.lang.IMPLEMENTATION_NAME
import com.squareup.sqldelight.core.lang.MAPPER_NAME
import com.squareup.sqldelight.core.lang.QUERY_LIST_TYPE
import com.squareup.sqldelight.core.lang.QUERY_TYPE
import com.squareup.sqldelight.core.lang.util.rawSqlText

class SelectQueryGenerator(private val query: NamedQuery) : QueryGenerator(query) {
  /**
   * The exposed query method which returns the default data class implementation.
   *
   * `fun selectForId(id: Int): Query<Data>`
   */
  fun defaultResultTypeFunction(): FunSpec {
    val function = FunSpec.builder(query.name).also(this::addJavadoc)
    val params = mutableListOf<CodeBlock>()
    query.arguments.sortedBy { it.index }.forEach { (_, argument) ->
      function.addParameter(argument.name, argument.argumentType())
      params.add(CodeBlock.of(argument.name))
    }
    params.add(CodeBlock.of("%T::$IMPLEMENTATION_NAME", query.interfaceType))
    return function
        .returns(QUERY_TYPE.parameterizedBy(query.interfaceType))
        .addStatement("return %L", params.joinToCode(", ", "${query.name}(", ")"))
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

    query.arguments.sortedBy { it.index }.forEach { (_, argument) ->
      // Adds each sqlite parameter to the argument list:
      // fun <T> selectForId(<<id>>, <<other_param>>, ...)
      function.addParameter(argument.name, argument.argumentType())
      params.add(CodeBlock.of(argument.name))
    }

    // Assemble the actual mapper lambda:
    // { resultSet ->
    //   mapper(
    //       resultSet.getLong(0),
    //       queryWrapper.tableAdapter.columnAdapter.decode(resultSet.getString(0))
    //   )
    // }
    val mapperLambda = CodeBlock.builder().addStatement(" { $CURSOR_NAME ->").indent()

    if (query.needsWrapper()) {
      if (query.needsLambda()) {
        // Function takes a custom mapper.

        // Add the type variable to the signature.
        val typeVariable = TypeVariableName("T", ANY)
        function.addTypeVariable(typeVariable)

        // Add the custom mapper to the signature:
        // mapper: (id: kotlin.Long, value: kotlin.String) -> T
        function.addParameter(ParameterSpec.builder(MAPPER_NAME, LambdaTypeName.get(
            parameters = query.resultColumns.map {
              ParameterSpec.builder(it.name, it.javaType)
                  .build()
            },
            returnType = typeVariable
        )).build())

        // Specify the return type for the mapper:
        // Query<T>
        function.returns(QUERY_TYPE.parameterizedBy(typeVariable))

        mapperLambda.add("$MAPPER_NAME(\n")
      } else {
        // Function only returns the interface type.
        // Query<SomeSelect>
        function.returns(QUERY_TYPE.parameterizedBy(query.interfaceType))
        mapperLambda.add("%T(\n", query.interfaceType.nestedClass(IMPLEMENTATION_NAME))
      }

      // Add the call of mapper with the deserialized columns:
      // mapper(
      //     resultSet.getLong(0),
      //     queryWrapper.tableAdapter.columnAdapter.decode(resultSet.getString(0))
      // )
      mapperLambda
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
      function.returns(QUERY_TYPE.parameterizedBy(query.resultColumns.single().javaType))
      mapperLambda.add(query.resultColumns.single().resultSetGetter(0)).add("\n")
    }
    mapperLambda.unindent().add("}\n")

    if (query.arguments.isEmpty()) {
      // No need for a custom query type, return an instance of Query:
      // return Query(statement, selectForId) { resultSet -> ... }
      function.addCode("return %T(${query.id}, ${query.name}, $DRIVER_NAME, %S)%L", QUERY_TYPE,
          query.statement.rawSqlText(), mapperLambda.build())
    } else {
      // Custom type is needed to handle dirtying events, return an instance of custom type:
      // return SelectForId(id) { resultSet -> ... }
      function.addCode("return %N(%L)%L", query.name.capitalize(),
          query.arguments.joinToString { (_, parameter) -> parameter.name }, mapperLambda.build())
    }

    return function.build()
  }

  /**
   * The private property used to delegate query result updates.
   *
   * `private val selectForId: MutableList<Query<*>> = mutableListOf()`
   */
  fun queryCollectionProperty(): PropertySpec {
    return PropertySpec.builder(query.name, QUERY_LIST_TYPE, INTERNAL)
        .initializer("com.squareup.sqldelight.internal.copyOnWriteList()")
        .build()
  }

  /**
   * The private query subtype for this specific query.
   *
   * ```
   * private class SelectForIdQuery<out T>(
   *   private val _id: Int,
   *   mapper: (SqlResultSet) -> T
   * ) : Query<T>(statement, selectForId, mapper) {
   * private inner class SelectForIdQuery<out T>(
   *   private val _id: Int, mapper: (Cursor) -> T
   * ): Query<T>(database.helper, selectForId, mapper)
   * ```
   */
  fun querySubtype(): TypeSpec {
    val queryType = TypeSpec.classBuilder(query.name.capitalize())
        .addModifiers(PRIVATE, INNER)

    val constructor = FunSpec.constructorBuilder()

    // The custom return type variable:
    // <out T>
    val returnType = TypeVariableName("T", bounds = *arrayOf(ANY), variance = OUT)
    queryType.addTypeVariable(returnType)

    // The superclass:
    // Query<T>
    queryType.superclass(QUERY_TYPE.parameterizedBy(returnType))

    val createStatementFunction = FunSpec.builder(EXECUTE_METHOD)
        .addModifiers(OVERRIDE)
        .returns(CURSOR_TYPE)
        .addCode(executeBlock())

    // For each bind argument the query has.
    query.arguments.sortedBy { it.index }.forEach { (_, parameter) ->
      // Add the argument as a constructor property. (Used later to figure out if query dirtied)
      // private val id: Int
      queryType.addProperty(PropertySpec.builder(parameter.name, parameter.argumentType(), PRIVATE)
          .initializer(parameter.name)
          .build())
      constructor.addParameter(parameter.name, parameter.argumentType())
    }

    // Add the query property to the super constructor
    queryType.addSuperclassConstructorParameter(query.name)

    // Add the mapper constructor parameter and pass to the super constructor
    constructor.addParameter(MAPPER_NAME, LambdaTypeName.get(
        parameters = *arrayOf(CURSOR_TYPE),
        returnType = returnType
    ))
    queryType.addSuperclassConstructorParameter(MAPPER_NAME)

    return queryType
        .primaryConstructor(constructor.build())
        .addFunction(createStatementFunction.build())
        .build()
  }
}
