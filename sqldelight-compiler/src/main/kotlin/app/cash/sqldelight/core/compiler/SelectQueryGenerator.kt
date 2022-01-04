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
package app.cash.sqldelight.core.compiler

import app.cash.sqldelight.core.compiler.SqlDelightCompiler.allocateName
import app.cash.sqldelight.core.compiler.model.NamedQuery
import app.cash.sqldelight.core.lang.ADAPTER_NAME
import app.cash.sqldelight.core.lang.CURSOR_NAME
import app.cash.sqldelight.core.lang.CURSOR_TYPE
import app.cash.sqldelight.core.lang.DRIVER_NAME
import app.cash.sqldelight.core.lang.EXECUTE_METHOD
import app.cash.sqldelight.core.lang.MAPPER_NAME
import app.cash.sqldelight.core.lang.QUERY_LISTENER_TYPE
import app.cash.sqldelight.core.lang.QUERY_TYPE
import app.cash.sqldelight.core.lang.psi.ColumnTypeMixin
import app.cash.sqldelight.core.lang.util.rawSqlText
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.INNER
import com.squareup.kotlinpoet.KModifier.OUT
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.NameAllocator
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.joinToCode

class SelectQueryGenerator(private val query: NamedQuery) : QueryGenerator(query) {
  /**
   * The exposed query method which returns the default data class implementation.
   *
   * `fun selectForId(id: Int): Query<Data>`
   */
  fun defaultResultTypeFunction(): FunSpec {
    val argNameAllocator = NameAllocator()
    val parametersAndTypes = query.arguments.sortedBy { it.index }
      .map { (_, arg) -> argNameAllocator.newName(arg.name, arg) to arg.argumentType() }

    val function = defaultResultTypeFunctionInterface(parametersAndTypes)
    val params = parametersAndTypes.map { (name) -> CodeBlock.of(name) }

    val columnArgs = query.resultColumns.map { argument ->
      argNameAllocator.newName(argument.name, argument)
    }

    val lamdaParams = columnArgs.joinToString(separator = ", ")
    val ctorParams = columnArgs.joinToString(separator = ",\n", postfix = "\n")

    val trailingLambda = CodeBlock.builder()
      .add(CodeBlock.of("·{ $lamdaParams ->\n"))
      .indent()
      .add("%T(\n", query.interfaceType)
      .indent()
      .add(ctorParams)
      .unindent()
      .add(")\n")
      .unindent()
      .add("}")
      .build()

    return function
      .addStatement(
        "return %L",
        CodeBlock
          .builder()
          .add(
            if (params.isEmpty()) {
              CodeBlock.of(query.name)
            } else {
              params.joinToCode(", ", "${query.name}(", ")")
            }
          )
          .add(trailingLambda)
          .build()
      )
      .build()
  }

  private fun defaultResultTypeFunctionInterface(params: List<Pair<String, TypeName>>): FunSpec.Builder {
    val function = FunSpec.builder(query.name)
      .also(this::addJavadoc)
    params.forEach { (name, type) ->
      function.addParameter(name, type)
    }
    return function
      .returns(QUERY_TYPE.parameterizedBy(query.interfaceType))
  }

  private fun customResultTypeFunctionInterface(): FunSpec.Builder {
    val function = FunSpec.builder(query.name)
    val params = mutableListOf<CodeBlock>()

    query.arguments.sortedBy { it.index }.forEach { (_, argument) ->
      // Adds each sqlite parameter to the argument list:
      // fun <T> selectForId(<<id>>, <<other_param>>, ...)
      function.addParameter(argument.name, argument.argumentType())
      params.add(CodeBlock.of(argument.name))
    }

    if (query.needsWrapper()) {
      // Function takes a custom mapper.

      // Add the type variable to the signature.
      val typeVariable = TypeVariableName("T", ANY)
      function.addTypeVariable(typeVariable)

      // Add the custom mapper to the signature:
      // mapper: (id: kotlin.Long, value: kotlin.String) -> T
      function.addParameter(
        ParameterSpec.builder(
          MAPPER_NAME,
          LambdaTypeName.get(
            parameters = query.resultColumns.map {
              ParameterSpec.builder(it.name, it.javaType.copy(annotations = emptyList()))
                .build()
            },
            returnType = typeVariable
          )
        ).build()
      )

      // Specify the return type for the mapper:
      // Query<T>
      function.returns(QUERY_TYPE.parameterizedBy(typeVariable))
    } else {
      // No custom type possible, just returns the single column:
      // fun selectSomeText(_id): Query<String>
      function.returns(QUERY_TYPE.parameterizedBy(query.resultColumns.single().javaType))
    }

    return function
  }

  /**
   * The exposed query method which returns a provided custom type.
   *
   * `fun <T> selectForId(id, mapper: (column1: String) -> T): Query<T>`
   */
  fun customResultTypeFunction(): FunSpec {
    val function = customResultTypeFunctionInterface()

    query.resultColumns.forEach { resultColumn ->
      (listOf(resultColumn) + resultColumn.assumedCompatibleTypes)
        .takeIf { it.size > 1 }
        ?.map { assumedCompatibleType ->
          (assumedCompatibleType.column?.columnType as ColumnTypeMixin?)?.let { columnTypeMixin ->
            val tableAdapterName = "${(assumedCompatibleType.column!!.parent as SqlCreateTableStmt).name()}$ADAPTER_NAME"
            val columnAdapterName = "${allocateName((columnTypeMixin.parent as SqlColumnDef).columnName)}$ADAPTER_NAME"
            "$tableAdapterName.$columnAdapterName"
          }
        }
        ?.let { adapterNames ->
          function.addStatement(
            """%M(%M(%L).size == 1) { "Adapter·types·are·expected·to·be·identical." }""",
            MemberName("kotlin", "check"),
            MemberName("kotlin.collections", "setOf"),
            adapterNames.joinToString()
          )
        }
    }

    // Assemble the actual mapper lambda:
    // { resultSet ->
    //   mapper(
    //       resultSet.getLong(0),
    //       tableAdapter.columnAdapter.decode(resultSet.getString(0))
    //   )
    // }
    val mapperLambda = CodeBlock.builder().addStatement("·{ $CURSOR_NAME ->").indent()

    if (query.needsWrapper()) {
      mapperLambda.add("$MAPPER_NAME(\n")

      // Add the call of mapper with the deserialized columns:
      // mapper(
      //     resultSet.getLong(0),
      //     tableAdapter.columnAdapter.decode(resultSet.getString(0))
      // )
      mapperLambda
        .indent()
        .apply {
          val decoders = query.resultColumns.mapIndexed { index, column -> column.cursorGetter(index) }
          add(decoders.joinToCode(separator = ",\n", suffix = "\n"))
        }
        .unindent()
        .add(")\n")
    } else {
      mapperLambda.add(query.resultColumns.single().cursorGetter(0)).add("\n")
    }
    mapperLambda.unindent().add("}\n")

    if (query.arguments.isEmpty()) {
      // No need for a custom query type, return an instance of Query:
      // return Query(statement, selectForId) { resultSet -> ... }
      function.addCode(
        "return %T(${query.id}, %L, $DRIVER_NAME, %S, %S, %S)%L",
        QUERY_TYPE, query.queryKeys(), query.statement.containingFile.name, query.name,
        query.statement.rawSqlText(), mapperLambda.build()
      )
    } else {
      // Custom type is needed to handle dirtying events, return an instance of custom type:
      // return SelectForId(id) { resultSet -> ... }
      function.addCode(
        "return %N(%L)%L", query.customQuerySubtype,
        query.arguments.joinToString { (_, parameter) -> parameter.name }, mapperLambda.build()
      )
    }

    return function.build()
  }

  /**
   * Either emptyArray() or arrayOf("table1", "table2")
   */
  private fun NamedQuery.queryKeys(): CodeBlock {
    return if (tablesObserved.isEmpty()) CodeBlock.of("emptyArray()")
    else tablesObserved.map { CodeBlock.of("\"${it.name}\"") }
      .joinToCode(", ", prefix = "arrayOf(", suffix = ")")
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
    val queryType = TypeSpec.classBuilder(query.customQuerySubtype)
      .addModifiers(PRIVATE, INNER)

    val constructor = FunSpec.constructorBuilder()

    // The custom return type variable:
    // <out T>
    val returnType = TypeVariableName("T", bounds = arrayOf(ANY), variance = OUT)
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
      // val id: Int
      queryType.addProperty(
        PropertySpec.builder(parameter.name, parameter.argumentType())
          .initializer(parameter.name)
          .build()
      )
      constructor.addParameter(parameter.name, parameter.argumentType())
    }

    // Add the mapper constructor parameter and pass to the super constructor
    constructor.addParameter(
      MAPPER_NAME,
      LambdaTypeName.get(
        parameters = arrayOf(CURSOR_TYPE),
        returnType = returnType
      )
    )
    queryType.addSuperclassConstructorParameter(MAPPER_NAME)

    if (query.tablesObserved.isNotEmpty()) {
      queryType
        .addFunction(
          FunSpec.builder("addListener")
            .addModifiers(OVERRIDE)
            .addParameter("listener", QUERY_LISTENER_TYPE)
            .addStatement("driver.addListener(listener, arrayOf(${query.tablesObserved.joinToString { "\"${it.name}\"" }}))")
            .build()
        )
        .addFunction(
          FunSpec.builder("removeListener")
            .addModifiers(OVERRIDE)
            .addParameter("listener", QUERY_LISTENER_TYPE)
            .addStatement("driver.removeListener(listener, arrayOf(${query.tablesObserved.joinToString { "\"${it.name}\"" }}))")
            .build()
        )
    }

    return queryType
      .primaryConstructor(constructor.build())
      .addFunction(createStatementFunction.build())
      .addFunction(
        FunSpec.builder("toString")
          .addModifiers(OVERRIDE)
          .returns(String::class)
          .addStatement("return %S", "${query.statement.containingFile.name}:${query.name}")
          .build()
      )
      .build()
  }
}
