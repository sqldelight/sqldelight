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

import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import com.squareup.sqldelight.FactorySpec.Companion.FACTORY_NAME
import com.squareup.sqldelight.model.isNullable
import com.squareup.sqldelight.resolution.query.QueryResults
import com.squareup.sqldelight.resolution.query.Table
import com.squareup.sqldelight.resolution.query.Value
import org.antlr.v4.runtime.ParserRuleContext
import java.util.LinkedHashMap
import javax.lang.model.element.Modifier

internal class MapperSpec private constructor() {
  private val factoryFields = linkedSetOf(Table.CREATOR_FIELD)

  fun tableMapper(table: Table) = table.generateMapper()

  private fun Table.generateMapper(): TypeSpec.Builder {
    val typeVariable = TypeVariableName.get("T", javaType)
    val mapper = TypeSpec.classBuilder(MAPPER_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .addTypeVariable(typeVariable)
        .addSuperinterface(ParameterizedTypeName.get(MAPPER_TYPE, typeVariable))
    val constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
    val factoryField = addFactoryField(mapper, constructor, javaType, typeVariable)
    val mapReturn = CodeBlock.builder().add("$[return $factoryField.${Table.CREATOR_FIELD}.create(\n")

    columns.forEachIndexed { i, column ->
      if (i != 0) mapReturn.add(",\n")
      mapReturn.add(column.cursorGetter(i))
    }

    val mapMethod = MethodSpec.methodBuilder("map")
        .addAnnotation(Override::class.java)
        .addModifiers(Modifier.PUBLIC)
        .returns(typeVariable)
        .addParameter(ParameterSpec.builder(CURSOR_TYPE, CURSOR_PARAM)
            .addAnnotation(SqliteCompiler.NON_NULL)
            .build())
        .addCode(mapReturn.add("$]\n);\n").build())

    return mapper.addMethod(constructor.build())
        .addMethod(mapMethod.build())
  }

  fun queryResultsMapper(queryResults: QueryResults) = queryResults.generateMapper()

  private fun QueryResults.generateMapper(): TypeSpec.Builder {
    val typeVariable = TypeVariableName.get("T", queryBound())
    val creatorType = ParameterizedTypeName.get(creatorType, *(types.values + typeVariable).toTypedArray())
    val mapper = TypeSpec.classBuilder(mapperName)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .addTypeVariables(types.values + typeVariable)
        .addSuperinterface(ParameterizedTypeName.get(MAPPER_TYPE, typeVariable))
        .addField(creatorType, Table.CREATOR_FIELD, Modifier.PRIVATE, Modifier.FINAL)

    if (javadoc != null) mapper.addJavadoc(javadoc)

    val constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(creatorType, Table.CREATOR_FIELD)
        .addStatement("this.${Table.CREATOR_FIELD} = ${Table.CREATOR_FIELD}")

    val mapMethod = MethodSpec.methodBuilder("map")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override::class.java)
        .addAnnotation(SqliteCompiler.NON_NULL)
        .returns(typeVariable)
        .addParameter(ParameterSpec.builder(CURSOR_TYPE, CURSOR_PARAM)
            .addAnnotation(SqliteCompiler.NON_NULL)
            .build())
        .addCode(CodeBlock.builder()
            .add("return ")
            .add(cursorGetter(mapper, constructor, rootCreator = true))
            .add(";\n")
            .build())

    return mapper.addMethod(constructor.build())
        .addMethod(mapMethod.build())
  }

  private fun addFactoryField(
      mapper: TypeSpec.Builder,
      constructor: MethodSpec.Builder,
      interfaceClass: ClassName,
      typeVariableName: TypeVariableName
  ): String {
    val factoryFieldname = "${interfaceClass.simpleName().decapitalize()}$FACTORY_NAME"
    if (!factoryFields.add(factoryFieldname)) return factoryFieldname
    val factoryType = ParameterizedTypeName.get(interfaceClass.nestedClass(FACTORY_NAME),
        typeVariableName)
    mapper.addField(factoryType, factoryFieldname, Modifier.PRIVATE, Modifier.FINAL)
    constructor.addParameter(factoryType, factoryFieldname)
        .addStatement("this.$factoryFieldname = $factoryFieldname")
    return factoryFieldname
  }

  private fun QueryResults.cursorGetter(
      mapper: TypeSpec.Builder,
      constructor: MethodSpec.Builder,
      types: MutableMap<TypeName, TypeVariableName> = LinkedHashMap(this.types),
      rootCreator: Boolean = false,
      index: Int = 0
  ): CodeBlock {
    val creatorField = if (rootCreator) Table.CREATOR_FIELD else "$originalViewName${Table.CREATOR_CLASS_NAME}"

    // For foreign tables we need to add their factory as a field so it can be used
    // during mapping. Note that values can also have foreign tables if they are
    // custom types grabbed from a foreign table.
    foreignTypes().forEachIndexed { index, foreignTable ->
      var typeVariable = types[foreignTable]
      if (typeVariable == null) {
        typeVariable = TypeVariableName.get("T${index+1}", foreignTable)!!
        mapper.addTypeVariable(typeVariable)
        types[foreignTable] = typeVariable
      }
      addFactoryField(mapper, constructor, foreignTable, typeVariable)
    }

    if (isView && factoryFields.add(creatorField)) {
      val creatorType = ParameterizedTypeName.get(
          creatorType,
          *(this.types.keys.map { types[it] } + types[javaType]).toTypedArray()
      )
      mapper.addField(creatorType, creatorField, Modifier.PRIVATE, Modifier.FINAL)
      constructor.addParameter(creatorType, creatorField)
          .addStatement("this.$creatorField = $creatorField")
    }
    val mapReturn = CodeBlock.builder()

    var extraIndents = false
    if (nullable) {
      // Whole table is nullable. If the first non null column is null, then the whole table
      // should be null. If there are no non null columns, assume the table is non null
      // and populate the individual columns.
      firstNonNull()?.let {
        extraIndents = true
        mapReturn.add("cursor.isNull(${index + it})\n")
            .indent().indent()
            .add("? null\n: ")
      }
    }

    mapReturn.add("$creatorField.create(\n")
        .indent().indent()
    results.fold(index, { columnIndex, result ->
      if (columnIndex != index) mapReturn.add(",\n")
      mapReturn.add(when (result) {
        is Value -> result.cursorGetter(columnIndex)
        is Table -> result.cursorGetter(columnIndex)
        is QueryResults -> result.cursorGetter(mapper, constructor, types, index = columnIndex)
        else -> throw IllegalStateException("Unknown result $result")
      })
      return@fold columnIndex + result.size()
    })
    mapReturn.unindent().unindent().add("\n)")
    if (extraIndents) mapReturn.unindent().unindent()

    return mapReturn.build()
  }

  private fun QueryResults.firstNonNull(): Int? {
    results.forEachIndexed { i, result ->
      when (result) {
        is Value -> if (!result.nullable) return i
        is QueryResults -> {
          val firstNullable = result.firstNonNull()
          if (firstNullable != null) return i + firstNullable
        }
        is Table -> {
          val firstNullable = result.columns.indexOfFirst { !(it.column?.isNullable ?: false) }
          if (firstNullable != -1) return i + firstNullable
        }
      }
    }
    return null
  }

  private fun Value.cursorGetter(index: Int): CodeBlock {
    val code = CodeBlock.builder()
    if (nullable) {
      code.add("$CURSOR_PARAM.isNull($index) ? null : ")
    }
    if (isHandledType) {
      code.add(handledTypeGetter(javaType, index, element))
    } else {
      code.add("${factoryField()}.$adapterField.decode(")
          .add(handledTypeGetter(dataType.defaultType, index, element))
          .add(")")
    }
    return code.build()
  }

  private fun Table.cursorGetter(index: Int): CodeBlock {
    val factoryField = "${javaType.simpleName().decapitalize()}$FACTORY_NAME"

    val code = CodeBlock.builder()

    var extraIndents = false
    if (nullable) {
      // Whole table is nullable. If the first non null column is null, then the whole table
      // should be null. If there are no non null columns, assume the table is non null
      // and populate the individual columns.
      val firstNonNull = columns.indexOfFirst { !(it.column?.isNullable ?: false) }
      if (firstNonNull != -1) {
        extraIndents = true
        code.add("$CURSOR_PARAM.isNull(${index + firstNonNull})\n")
            .indent().indent()
            .add("? null\n: ")
      }
    }

    code.add("$factoryField.${Table.CREATOR_FIELD}.${Table.CREATOR_METHOD_NAME}(\n")
        .indent().indent()
    columns.forEachIndexed { i, column ->
      if (i != 0) code.add(",\n")
      code.add(column.cursorGetter(index + i))
    }
    code.unindent().unindent().add("\n)")
    if (extraIndents) code.unindent().unindent()

    return code.build()
  }

  companion object {
    internal val CURSOR_TYPE = ClassName.get("android.database", "Cursor")
    internal val CURSOR_PARAM = "cursor"
    internal val MAPPER_NAME = "Mapper"
    internal val MAPPER_TYPE = ClassName.get("com.squareup.sqldelight", "RowMapper")

    internal fun handledTypeGetter(
        javaType: TypeName,
        index: Int,
        element: ParserRuleContext
    ): CodeBlock {
      if (javaType == TypeName.BOOLEAN || javaType == TypeName.BOOLEAN.box()) {
        return CodeBlock.builder().add("$CURSOR_PARAM.getInt($index) == 1").build()
      }
      if (javaType == TypeName.INT || javaType == TypeName.INT.box()) {
        return CodeBlock.builder().add("$CURSOR_PARAM.getInt($index)").build()
      }
      if (javaType == TypeName.SHORT || javaType == TypeName.SHORT.box()) {
        return CodeBlock.builder().add("$CURSOR_PARAM.getShort($index)").build()
      }
      if (javaType == TypeName.LONG || javaType == TypeName.LONG.box()) {
        return CodeBlock.builder().add("$CURSOR_PARAM.getLong($index)").build()
      }
      if (javaType == TypeName.FLOAT || javaType == TypeName.FLOAT.box()) {
        return CodeBlock.builder().add("$CURSOR_PARAM.getFloat($index)").build()
      }
      if (javaType == TypeName.DOUBLE || javaType == TypeName.DOUBLE.box()) {
        return CodeBlock.builder().add("$CURSOR_PARAM.getDouble($index)").build()
      }
      if (javaType == ArrayTypeName.of(TypeName.BYTE)) {
        return CodeBlock.builder().add("$CURSOR_PARAM.getBlob($index)").build()
      }
      if (javaType == ClassName.get(String::class.java)) {
        return CodeBlock.builder().add("$CURSOR_PARAM.getString($index)").build()
      }
      if (javaType == TypeName.VOID.box()) {
        return CodeBlock.builder().add("null").build()
      }
      throw SqlitePluginException(element, "Unknown cursor getter for type $javaType")
    }

    internal fun builder(queryResults: QueryResults) = MapperSpec().queryResultsMapper(queryResults)
    internal fun builder(table: Table) = MapperSpec().tableMapper(table)
  }
}
