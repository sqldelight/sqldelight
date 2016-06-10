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
package com.squareup.sqldelight

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.NameAllocator
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import com.squareup.sqldelight.model.Table
import com.squareup.sqldelight.model.adapterField
import com.squareup.sqldelight.model.adapterType
import com.squareup.sqldelight.model.isHandledType
import com.squareup.sqldelight.model.isNullable
import com.squareup.sqldelight.validation.QueryResults
import java.util.ArrayList
import java.util.LinkedHashSet
import javax.lang.model.element.Modifier

internal class FactorySpec(
    private val table: Table?,
    private val queryResultsList: List<QueryResults>,
    private val interfaceType: ClassName,
    private val nameAllocator: NameAllocator
) {
  fun build(): TypeSpec {
    val typeSpec = TypeSpec.classBuilder(FACTORY_NAME)

    if (table != null) {
      typeSpec.addTypeVariable(TypeVariableName.get("T", table.interfaceClassName))
          .addField(table.creatorType, Table.CREATOR_FIELD, Modifier.PUBLIC, Modifier.FINAL)
          .addFields(table.column_def().filter { !it.isHandledType }.map { column ->
            FieldSpec.builder(column.adapterType(), column.adapterField(table.nameAllocator))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .build()
          })
    }

    queryResultsList.forEach {
      var queryResults = it
      val mapperMethod = MethodSpec.methodBuilder(queryResults.mapperName.decapitalize())
          .addModifiers(Modifier.PUBLIC)
      val mapperType: ClassName
      val typeVariables = ArrayList<TypeVariableName>()

      if (queryResults.singleView) {
        queryResults = queryResults.views.values.first()
      }

      if (queryResults.requiresType) {
        mapperMethod.addTypeVariable(TypeVariableName.get("R", queryResults.interfaceType))
            .addParameter(ParameterizedTypeName.get(queryResults.creatorType,
                TypeVariableName.get("R")), Table.CREATOR_FIELD)
        typeVariables.add(TypeVariableName.get("R"))
        mapperType = queryResults.mapperType
      } else if (queryResults.tables.size == 1) {
        mapperType = queryResults.tables.values.first().interfaceType.nestedClass(MapperSpec.MAPPER_NAME)
      } else {
        typeSpec.addMethod(queryResults.singleValueMapper())
        return@forEach
      }

      val params = queryResults.mapperParameters(mapperMethod, typeVariables, interfaceType, isRoot = true)
      if (queryResults.requiresType) params.add(0, Table.CREATOR_FIELD)

      typeSpec.addMethod(mapperMethod
          .returns(ParameterizedTypeName.get(mapperType, *typeVariables.toTypedArray()))
          .addStatement("return new \$T<>(${params.joinToString()})", mapperType)
          .build())
    }

    return typeSpec
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .addMethod(constructor())
        .build()
  }

  /**
   * Mutates the given method to include the required factories/creators for this query result.
   * Returns a list of the factory/creator parameter names.
   */
  private fun QueryResults.mapperParameters(
      mapperMethod: MethodSpec.Builder,
      typeVariables: ArrayList<TypeVariableName>,
      factoryType: ClassName,
      paramNames: LinkedHashSet<String> = LinkedHashSet<String>(),
      typePrefix: String = "",
      isRoot: Boolean = false
  ): ArrayList<String> {
    val result = ArrayList<String>()
    foreignTypes().forEachIndexed { i, foreignTable ->
      if (foreignTable == factoryType) {
        if (!paramNames.add("this")) return@forEachIndexed
        result.add("this")
        typeVariables.add(TypeVariableName.get("T"))
      } else {
        val factoryParam = "${foreignTable.simpleName().decapitalize()}$FACTORY_NAME"
        if (!paramNames.add(factoryParam)) return@forEachIndexed
        mapperMethod.addParameter(ParameterizedTypeName.get(
            foreignTable.nestedClass(FACTORY_NAME), TypeVariableName.get("${typePrefix}R${i+1}")),
            factoryParam)
        mapperMethod.addTypeVariable(TypeVariableName.get("${typePrefix}R${i+1}", foreignTable))
        typeVariables.add(TypeVariableName.get("${typePrefix}R${i+1}"))
        result.add(factoryParam)
      }
    }

    if (!isRoot) {
      // Add the view creator as a parameter.
      val creatorField = "$queryName${Table.CREATOR_CLASS_NAME}"
      if (paramNames.add(creatorField)) {
        val type = ParameterizedTypeName.get(creatorType, TypeVariableName.get(typePrefix))
        typeVariables.add(TypeVariableName.get(typePrefix))
        mapperMethod.addParameter(type, "$queryName${Table.CREATOR_CLASS_NAME}")
        mapperMethod.addTypeVariable(TypeVariableName.get(typePrefix, interfaceType))
        result.add(creatorField)
      }
    }

    views.entries.forEachIndexed { i, entry ->
      result.addAll(entry.value.mapperParameters(mapperMethod, typeVariables, factoryType,
          paramNames, "${typePrefix}V${i+1}"))
    }

    return result
  }

  private fun QueryResults.singleValueMapper(): MethodSpec {
    val indexedValue = columns.values.first()
    val mapperMethod = MethodSpec.methodBuilder(mapperName.decapitalize())
        .addModifiers(Modifier.PUBLIC)
        .returns(ParameterizedTypeName.get(MapperSpec.MAPPER_TYPE, indexedValue.javaType.box()))

    val rowMapper = TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(ParameterizedTypeName.get(MapperSpec.MAPPER_TYPE,
            indexedValue.javaType.box()))
        .addMethod(MethodSpec.methodBuilder(MapperSpec.MAP_FUNCTION)
            .addAnnotation(Override::class.java)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(MapperSpec.CURSOR_TYPE, MapperSpec.CURSOR_PARAM)
            .addCode(indexedValue.singleValueReturn(mapperMethod))
            .returns(indexedValue.javaType.box())
            .build())
        .build()

    return mapperMethod
        .addStatement("return \$L", rowMapper)
        .build()
  }

  private fun QueryResults.IndexedValue.singleValueReturn(mapperMethod: MethodSpec.Builder): CodeBlock {
    val returnStatement = CodeBlock.builder().add("return ")
    if (value.columnDef?.isNullable ?: true) {
      returnStatement.add("${MapperSpec.CURSOR_PARAM}.isNull(0) ? null : ")
    }
    if (value.columnDef?.isHandledType ?: true) {
      returnStatement.add(MapperSpec.handledTypeGetter(javaType, 0, value.element))
    } else {
      val factoryParam = "${tableInterface!!.simpleName().decapitalize()}$FACTORY_NAME"
      mapperMethod.addTypeVariable(TypeVariableName.get("T", tableInterface))
          .addParameter(ParameterizedTypeName.get(tableInterface.nestedClass(FACTORY_NAME),
              TypeVariableName.get("T")), factoryParam, Modifier.FINAL)
      returnStatement.add("$factoryParam.${value.columnDef!!.adapterField(nameAllocator)}." +
          "${MapperSpec.MAP_FUNCTION}(${MapperSpec.CURSOR_PARAM}, 0)")
    }
    return returnStatement.addStatement("").build()
  }

  private fun constructor(): MethodSpec {
    val constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)

    if (table != null) {
      val nameAllocator = table.nameAllocator
      constructor.addParameter(table.creatorType, Table.CREATOR_FIELD)
          .addStatement("this.${Table.CREATOR_FIELD} = ${Table.CREATOR_FIELD}")

      table.column_def().filter { !it.isHandledType }.forEach { column ->
        constructor.addParameter(column.adapterType(), column.adapterField(nameAllocator))
            .addStatement("this.${column.adapterField(nameAllocator)} = ${column.adapterField(
                nameAllocator)}")
      }
    }

    return constructor.build()
  }

  companion object {
    const val FACTORY_NAME = "Factory"

    internal fun builder(
        table: Table?,
        queryResultsList: List<QueryResults>,
        interfaceType: ClassName,
        nameAllocator: NameAllocator
    ) = FactorySpec(table, queryResultsList, interfaceType, nameAllocator)
  }
}
