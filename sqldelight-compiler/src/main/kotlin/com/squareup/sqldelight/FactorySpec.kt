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
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
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
    private val nameAllocators: MutableMap<String, NameAllocator>
) {
  fun build(): TypeSpec {
    val typeSpec = TypeSpec.classBuilder(FACTORY_NAME)

    if (table != null) {
      val marshalClassName = table.interfaceClassName.nestedClass("Marshal")
      typeSpec.addTypeVariable(TypeVariableName.get("T", table.interfaceClassName))
          .addField(table.creatorType, Table.CREATOR_FIELD, Modifier.PUBLIC, Modifier.FINAL)
          .addFields(table.column_def().filter { !it.isHandledType }.map { column ->
            FieldSpec.builder(column.adapterType(), column.adapterField(table.nameAllocator))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .build()
          })
          .addMethod(MethodSpec.methodBuilder(MARSHAL_METHOD)
              .addModifiers(Modifier.PUBLIC)
              .returns(marshalClassName)
              .addStatement(
                  "return new \$T(\$L)",
                  marshalClassName,
                  listOf("null").plus(table.column_def().filter { !it.isHandledType }.map {
                    it.adapterField(table.nameAllocator)
                  }).joinToString()
              )
              .build())
          .addMethod(MethodSpec.methodBuilder(MARSHAL_METHOD)
              .addModifiers(Modifier.PUBLIC)
              .returns(marshalClassName)
              .addParameter(ParameterSpec.builder(table.interfaceClassName, COPY_PARAM).build())
              .addStatement(
                  "return new \$T(\$L)",
                  marshalClassName,
                  listOf(COPY_PARAM).plus(table.column_def().filter { !it.isHandledType }.map {
                    it.adapterField(table.nameAllocator)
                  }).joinToString()
              )
              .build())
    }

    queryResultsList.forEach {
      var queryResults = it
      val mapperMethod = MethodSpec.methodBuilder(queryResults.mapperName.decapitalize())
          .addModifiers(Modifier.PUBLIC)
      val mapperType: ClassName

      if (queryResults.singleView) {
        queryResults = queryResults.views.values.first()
      }

      val types = factoryTypes(queryResults)
      val typeVariables = ArrayList<TypeVariableName>(types.values)
      mapperMethod.addTypeVariables(types.filterKeys { it != interfaceType }.values)

      if (queryResults.requiresType) {
        val typeVariable = TypeVariableName.get("R", queryResults.queryBound(types))
        typeVariables.add(typeVariable)
        mapperType = queryResults.mapperType
        mapperMethod.addTypeVariable(typeVariable)
            .addParameter(ParameterizedTypeName.get(queryResults.creatorType,
                *typeVariables.toTypedArray()), Table.CREATOR_FIELD)
      } else if (queryResults.tables.size == 1) {
        mapperType = queryResults.tables.values.first().interfaceType.nestedClass(MapperSpec.MAPPER_NAME)
      } else {
        typeSpec.addMethod(queryResults.singleValueMapper())
        return@forEach
      }

      val params = queryResults.mapperParameters(mapperMethod, typeVariables, interfaceType, isRoot = true, types = types)
      if (queryResults.requiresType) params.add(0, Table.CREATOR_FIELD)

      val parameterizedMapperType = ParameterizedTypeName.get(mapperType, *typeVariables.toTypedArray())
      typeSpec.addMethod(mapperMethod
          .returns(parameterizedMapperType)
          .addStatement("return new \$T(${params.joinToString()})", parameterizedMapperType)
          .build())
    }

    return typeSpec
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .addMethod(constructor())
        .build()
  }

  /**
   * Returns a modified version of the original QueryResults types map where any mention of
   * the table this factory is inner classed on is replaced with the type variable "T" which
   * is placed on this factory.
   */
  private fun factoryTypes(queryResults: QueryResults) = queryResults.types.mapValues {
    if (it.key == interfaceType) {
      TypeVariableName.get("T")
    } else if (it.value.bounds.first() is ParameterizedTypeName) {
      val originalType = it.value.bounds.first() as ParameterizedTypeName
      TypeVariableName.get(
          it.value.name,
          ParameterizedTypeName.get(
              originalType.rawType,
              *originalType.typeArguments.map {
                if ((it as TypeVariableName).bounds.first() == interfaceType) {
                  TypeVariableName.get("T")
                } else {
                  it
                }
              }.toTypedArray()
          )
      )
    } else {
      it.value
    }
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
      types: Map<TypeName, TypeVariableName> = this.types,
      isRoot: Boolean = false
  ): ArrayList<String> {
    val result = ArrayList<String>()
    for ((index, foreignTable) in foreignTypes()) {
      if (foreignTable == factoryType) {
        if (!paramNames.add("this")) continue
        result.add("this")
        if (!types.values.any { it.name == "T" }) {
          typeVariables.add(TypeVariableName.get("T"))
        }
      } else {
        val factoryParam = "${foreignTable.simpleName().decapitalize()}$FACTORY_NAME"
        if (!paramNames.add(factoryParam)) continue
        var typeVariable = types[foreignTable]
        if (typeVariable == null) {
          typeVariable = TypeVariableName.get("T${index+1}", foreignTable)!!
          mapperMethod.addTypeVariable(typeVariable)
          typeVariables.add(typeVariable)
        }
        mapperMethod.addParameter(ParameterizedTypeName.get(foreignTable.nestedClass(FACTORY_NAME),
            typeVariable), factoryParam)
        result.add(factoryParam)
      }
    }

    if (!isRoot) {
      // Add the view creator as a parameter.
      val creatorField = "$queryName${Table.CREATOR_CLASS_NAME}"
      if (paramNames.add(creatorField)) {
        val creatorType = ParameterizedTypeName.get(
            creatorType,
            *(this.types.keys.map { types[it] } + types[interfaceType]).toTypedArray()
        )
        mapperMethod.addParameter(creatorType, "$queryName${Table.CREATOR_CLASS_NAME}")
        result.add(creatorField)
      }
    }

    views.entries.sortedBy { it.value.index }.forEachIndexed { i, entry ->
      result.addAll(entry.value.mapperParameters(mapperMethod, typeVariables, factoryType,
          paramNames, types))
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
      val adapterField = value.columnDef!!.adapterField(nameAllocators.getOrPut(tableName!!, { NameAllocator() }))
      returnStatement.add("$factoryParam.$adapterField.${MapperSpec.MAP_FUNCTION}(${MapperSpec.CURSOR_PARAM}, 0)")
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
    const val MARSHAL_METHOD = "marshal"
    const val COPY_PARAM = "copy"

    internal fun builder(
        table: Table?,
        queryResultsList: List<QueryResults>,
        interfaceType: ClassName,
        nameAllocators: MutableMap<String, NameAllocator>
    ) = FactorySpec(table, queryResultsList, interfaceType, nameAllocators)
  }
}
