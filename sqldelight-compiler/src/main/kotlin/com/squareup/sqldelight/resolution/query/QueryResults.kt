/*
 * Copyright (C) 2016 Square, Inc.
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
package com.squareup.sqldelight.resolution.query

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.NameAllocator
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import com.squareup.sqldelight.MapperSpec
import com.squareup.sqldelight.model.isHandledType
import org.antlr.v4.runtime.ParserRuleContext
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import javax.lang.model.element.Modifier

/**
 * Corresponds to a single select statement in SQL, including subqueries and views.
 */
data class QueryResults private constructor(
    override val name: String,
    override val element: ParserRuleContext,
    override val nullable: Boolean,
    val results: List<Result>,
    val modelInterface: ClassName?,
    val isView: Boolean,
    internal val originalViewName: String = name
) : Result {
  internal val types: Map<TypeName, TypeVariableName>
  internal val interfaceClassName: String
    get() = "${originalViewName.capitalize()}Model"
  override var javaType: TypeName = TypeName.VOID
    get() = modelInterface!!.nestedClass(interfaceClassName)
  internal val creatorType: ClassName
    get() = modelInterface!!.nestedClass("${originalViewName.capitalize()}Creator")
  internal val mapperName: String
    get() = "${originalViewName.capitalize()}${MapperSpec.MAPPER_NAME}"
  internal val mapperType: ClassName
    get() = modelInterface!!.nestedClass(mapperName)
  internal val requiresType = results.size > 1 || isView
  internal val singleView = results.size == 1 && results.firstOrNull() is QueryResults

  private val nameAllocator = NameAllocator()

  constructor(
      tableName: ParserRuleContext,
      results: List<Result>,
      tableInterface: ClassName? = null,
      isView: Boolean = false
  ) : this(
      tableName.text,
      tableName,
      false,
      results.flatMap { result ->
        when (result) {
          is QueryResults -> {
            if (!result.isView) return@flatMap result.results
          }
        }
        return@flatMap listOf(result)
      },
      tableInterface,
      isView
  )

  init {
    // Initialize the types map.
    val types = LinkedHashMap<TypeName, TypeVariableName>()
    results.forEachIndexed { index, result ->
      when (result) {
        is Table -> {
          types.getOrPut(result.javaType,
              { TypeVariableName.get("T${index + 1}", result.javaType) })
        }
        is QueryResults -> {
          // For each type we are adding to satisfy the view, we have to re-do its bounds to
          // whatever this QueryResult has already generated types for.
          result.types.forEach {
            val bound = it.value.bounds.first()
            val newBound: TypeName
            if (bound is ClassName) {
              // Table or parameterless view - add the type to our map as if it were our own table.
              newBound = bound
            } else if (bound is ParameterizedTypeName) {
              // View - check the type arguments, which are guaranteed TypeVariableNames (see below
              // where we add the view itself) and use the TypeVariableName found in our own map
              // instead.
              newBound = ParameterizedTypeName.get(
                  bound.rawType,
                  *bound.typeArguments.map { types[(it as TypeVariableName).bounds.first()] }.toTypedArray()
              )
            } else {
              throw IllegalStateException("Unexpected type variable $bound")
            }
            types.getOrPut(it.key,
                { TypeVariableName.get("V${index + 1}${it.value.name}", newBound) })
          }
          // Add the type for the view itself.
          types.getOrPut(result.javaType,
              { TypeVariableName.get("V${index + 1}", result.queryBound(types)) })
        }
      }
    }
    this.types = types
  }

  /**
   * For the given QueryResults, form a TypeVariable that has bounds corresponding
   * to the current instance's type map. However if the QueryResults passed in has no types
   * associated with it, the bound is just the type itself (it is not Parameterized).
   */
  internal fun queryBound(types: Map<TypeName, TypeVariableName> = this.types) =
      if (this.types.isEmpty()) {
        javaType
      } else {
        ParameterizedTypeName.get(
            javaType as ClassName,
            *this.types.keys.map { types[it] }.toTypedArray()
        )
      }

  private fun Result.name() =
    try {
      nameAllocator.get(this)
    } catch (e: IllegalArgumentException) {
      nameAllocator.newName(name, this)
    }

  internal fun generateInterface() = TypeSpec.interfaceBuilder(interfaceClassName)
      .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
      .addTypeVariables(types.values)
      .addMethods(results.map {
        MethodSpec.methodBuilder(it.name())
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addAnnotations(it.annotations())
            .returns(localType(it))
            .build()
      })
      .build()

  internal fun generateCreator() = TypeSpec.interfaceBuilder("${originalViewName.capitalize()}Creator")
      .addTypeVariables(types.values + TypeVariableName.get("T", queryBound()))
      .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
      .addMethod(MethodSpec.methodBuilder("create")
          .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
          .addParameters(results.map {
            ParameterSpec.builder(localType(it), it.name()).addAnnotations(it.annotations()).build()
          })
          .returns(TypeVariableName.get("T"))
          .build())
      .build()

  internal fun foreignTypes() = results.map {
    when (it) {
      is Table -> it.javaType
      is Value -> if (!(it.column?.isHandledType ?: true)) it.tableInterface else null
      else -> null
    }
  }
      .distinct()
      .filterNotNull()

  private fun localType(result: Result): TypeName {
    val type = types[result.javaType] ?: result.javaType
    // TODO: Remove try-catch when we update to javapoet latest.
    try {
      if (result.nullable) {
        return type.box()
      }
      return type.unbox()
    } catch (e: UnsupportedOperationException) {
      return type
    }
  }

  /**
   * Returns a new QueryResults object with result method names modified to eliminate duplicates.
   */
  internal fun modifyDuplicates(): QueryResults {
    val names = LinkedHashSet<String>()
    return copy(results = results.map { result ->
      var index = 1
      var newResult = result
      while (!names.add(newResult.name)) {
        if (result is Value && result.tableName != null) {
          // Before adding a number, try prepending the table name if there is one.
          if (names.add("${result.tableName}_${newResult.name}")) {
            return@map result.copy(name = "${result.tableName}_${newResult.name}")
          }
        }
        when (result) {
          is Table -> newResult = result.copy(name = "${result.name}_${++index}")
          is QueryResults -> newResult = result.copy(name = "${result.name}_${++index}")
          is Value -> newResult = result.copy(name = "${result.name}_${++index}")
        }
      }
      return@map newResult
    })
  }

  override fun columnNames() = results.flatMap { it.columnNames() }
  override fun tableNames() = listOf(name)
  override fun size() = results.fold(0, { size, result -> size + result.size() })
  override fun expand() = results.flatMap { it.expand() }
  override fun findElement(columnName: String, tableName: String?) =
      if (tableName == null || tableName == name) results.flatMap { it.findElement(columnName) }
      else emptyList()
}
