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
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import com.squareup.sqldelight.model.SqlStmt
import com.squareup.sqldelight.resolution.query.QueryResults
import com.squareup.sqldelight.resolution.query.Table
import com.squareup.sqldelight.resolution.query.Value
import com.squareup.sqldelight.resolution.query.foreignValues
import java.util.ArrayList
import java.util.LinkedHashSet
import javax.lang.model.element.Modifier

internal class FactorySpec(
    private val table: Table?,
    private val queryResultsList: List<QueryResults>,
    private val sqlStmts: List<SqlStmt>,
    private val interfaceType: ClassName
) {
  // A collection of all type variables used in the Factory type.
  private val factoryTypes = linkedMapOf(interfaceType to Factory("this", TypeVariableName.get("T")))

  fun build(): TypeSpec {
    val factoryTypeName = interfaceType.nestedClass(FACTORY_NAME)
    val typeSpec = TypeSpec.classBuilder(factoryTypeName)
    typeSpec.addMethod(constructor(typeSpec))

    if (table != null) {
      typeSpec.addTypeVariable(TypeVariableName.get("T", table.javaType))
          .addField(table.creatorType, Table.CREATOR_FIELD, Modifier.PUBLIC, Modifier.FINAL)
          .addFields(table.columns.filter { !it.isHandledType }.map { column ->
            FieldSpec.builder(column.adapterType, column.adapterField)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .build()
          })
    }

    sqlStmts.filter { it.needsSqlDelightStatement }
        .filterNot { it.needsCompiledStatement }
        .forEach {
          val queryTypeName = factoryTypeName.nestedClass(it.name.capitalize() + "Query")
          it.factoryQueryType(interfaceType, queryTypeName)?.let { typeSpec.addType(it) }
          typeSpec.addMethod(it.factoryQueryMethod(interfaceType, queryTypeName))
        }

    queryResultsList.forEach {
      var queryResults = it
      val mapperMethod = MethodSpec.methodBuilder(queryResults.mapperName.decapitalize())
          .addModifiers(Modifier.PUBLIC)
      val mapperType: ClassName

      if (queryResults.singleView) {
        queryResults = queryResults.results.first() as QueryResults
      }

      val types = factoryTypes(queryResults)
      val typeVariables = ArrayList<TypeVariableName>(types.values)
      mapperMethod.addTypeVariables(types.filterKeys { !factoryTypes.containsKey(it) }.values)

      val firstResult = queryResults.results.first()
      if (queryResults.requiresType) {
        val typeVariable = TypeVariableName.get("R", queryResults.queryBound(types))
        typeVariables.add(typeVariable)
        mapperType = queryResults.mapperType
        mapperMethod.addTypeVariable(typeVariable)
            .addParameter(ParameterizedTypeName.get(queryResults.creatorType,
                *typeVariables.toTypedArray()), Table.CREATOR_FIELD)
      } else if (queryResults.results.size == 1 && firstResult is com.squareup.sqldelight.resolution.query.Table) {
        mapperType = firstResult.javaType.nestedClass(MapperSpec.MAPPER_NAME)
      } else {
        typeSpec.addMethod(queryResults.singleValueMapper())
        return@forEach
      }

      val params = queryResults.mapperParameters(mapperMethod, typeVariables, interfaceType, isRoot = true, types = types)
      if (queryResults.requiresType) params.add(0, Table.CREATOR_FIELD)

      val parameterizedMapperType = ParameterizedTypeName.get(mapperType, *typeVariables.toTypedArray())
      if (it.javadoc != null) {
        mapperMethod.addJavadoc(it.javadoc)
      }
      typeSpec.addMethod(mapperMethod
          .returns(parameterizedMapperType)
          .addStatement("return new \$T(${params.joinToString()})", parameterizedMapperType)
          .build())
    }

    return typeSpec
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .build()
  }

  /**
   * Returns a modified version of the original QueryResults types map where any mention of
   * the table this factory is inner classed on is replaced with the type variable "T" which
   * is placed on this factory.
   */
  private fun factoryTypes(queryResults: QueryResults) = queryResults.types.mapValues {
    if (factoryTypes.containsKey(it.key)) {
      factoryTypes[it.key]!!.typeVariable
    } else if (it.value.bounds.first() is ParameterizedTypeName) {
      val originalType = it.value.bounds.first() as ParameterizedTypeName
      TypeVariableName.get(
          it.value.name,
          ParameterizedTypeName.get(
              originalType.rawType,
              *originalType.typeArguments.map {
                if (factoryTypes.containsKey((it as TypeVariableName).bounds.first())) {
                  factoryTypes[it.bounds.first()]!!.typeVariable
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
    foreignTypes().forEachIndexed { index, foreignTable ->
      val localFactoryType = factoryTypes[foreignTable]
      if (localFactoryType != null) {
        if (!paramNames.add(localFactoryType.field)) return@forEachIndexed
        result.add(localFactoryType.field)
        if (!types.values.any { it.name == localFactoryType.typeVariable.name }) {
          typeVariables.add(localFactoryType.typeVariable)
        }
      } else {
        val factoryParam = "${foreignTable.simpleName().decapitalize()}$FACTORY_NAME"
        if (!paramNames.add(factoryParam)) return@forEachIndexed
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
      val creatorField = "$originalViewName${Table.CREATOR_CLASS_NAME}"
      if (paramNames.add(creatorField)) {
        val creatorType = ParameterizedTypeName.get(
            creatorType,
            *(this.types.keys.map { types[it] } + types[javaType]).toTypedArray()
        )
        mapperMethod.addParameter(creatorType, "$originalViewName${Table.CREATOR_CLASS_NAME}")
        result.add(creatorField)
      }
    }

    results.filterIsInstance<QueryResults>().forEach {
      result.addAll(it.mapperParameters(mapperMethod, typeVariables, factoryType, paramNames, types))
    }

    return result
  }

  private fun QueryResults.singleValueMapper(): MethodSpec {
    val value = results.first() as Value
    val mapperMethod = MethodSpec.methodBuilder(mapperName.decapitalize())
        .addModifiers(Modifier.PUBLIC)
        .returns(ParameterizedTypeName.get(MapperSpec.MAPPER_TYPE, value.javaType.box()))

    val rowMapper = TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(ParameterizedTypeName.get(MapperSpec.MAPPER_TYPE,
            value.javaType.box()))
        .addMethod(MethodSpec.methodBuilder("map")
            .addAnnotation(Override::class.java)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(MapperSpec.CURSOR_TYPE, MapperSpec.CURSOR_PARAM)
            .addCode(value.singleValueReturn(mapperMethod))
            .returns(value.javaType.box())
            .build())
        .build()

    return mapperMethod
        .addStatement("return \$L", rowMapper)
        .build()
  }

  private fun Value.singleValueReturn(mapperMethod: MethodSpec.Builder): CodeBlock {
    val returnStatement = CodeBlock.builder().add("return ")
    if (nullable) {
      returnStatement.add("${MapperSpec.CURSOR_PARAM}.isNull(0) ? null : ")
    }
    if (isHandledType) {
      returnStatement.add(MapperSpec.handledTypeGetter(javaType, 0, element))
    } else {
      if (tableInterface!! == interfaceType) {
        // We already have the needed adapter in the factory.
        returnStatement.add("$adapterField.decode(")
      } else {
        // Requires an adapter from a external factory.
        mapperMethod.addTypeVariable(TypeVariableName.get("T", tableInterface))
            .addParameter(ParameterizedTypeName.get(tableInterface.nestedClass(FACTORY_NAME),
                TypeVariableName.get("T")), factoryField(), Modifier.FINAL)
        returnStatement.add("${factoryField()}.$adapterField.decode(")
      }
      returnStatement.add(MapperSpec.handledTypeGetter(dataType.defaultType, 0, element)).add(")")
    }
    return returnStatement.addStatement("").build()
  }

  private fun constructor(typeSpec: TypeSpec.Builder): MethodSpec {
    val constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)

    if (table != null) {
      constructor.addParameter(table.creatorType, Table.CREATOR_FIELD)
          .addStatement("this.${Table.CREATOR_FIELD} = ${Table.CREATOR_FIELD}")

      table.columns.filter { !it.isHandledType }.forEach { column ->
        constructor.addParameter(column.adapterType, column.adapterField)
            .addStatement("this.${column.adapterField} = ${column.adapterField}")
      }
    } else {
      // If table is null then we want to add any factories required by mappers/statements ahead
      // of time.
      sqlStmts.flatMap {
            it.arguments.map { it.argumentType.comparable }.foreignValues().map { it.tableInterface }
          }
          .plus(queryResultsList.flatMap { it.foreignTypes() })
          .filterNotNull()
          .distinct()
          .forEachIndexed { index, foreignTable ->
            val factoryParam = "${foreignTable.simpleName().decapitalize()}$FACTORY_NAME"
            val typeVariable = TypeVariableName.get("T${index + 1}", foreignTable)!!
            val factoryType = ParameterizedTypeName.get(foreignTable.nestedClass(FACTORY_NAME), typeVariable)
            factoryTypes.put(foreignTable, Factory(factoryParam, typeVariable))
            typeSpec.addTypeVariable(typeVariable)
                .addField(factoryType, factoryParam)
            constructor.addParameter(factoryType, factoryParam)
                .addStatement("this.$factoryParam = $factoryParam")
          }
    }

    return constructor.build()
  }

  data class Factory(val field: String, val typeVariable: TypeVariableName)

  companion object {
    const val FACTORY_NAME = "Factory"

    internal fun builder(
        table: Table?,
        status: Status.ValidationStatus.Validated,
        interfaceType: ClassName
    ) = FactorySpec(table, status.queries, status.sqlStmts, interfaceType)
  }
}
