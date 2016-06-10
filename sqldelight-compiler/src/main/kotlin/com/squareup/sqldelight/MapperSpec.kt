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
import com.squareup.javapoet.NameAllocator
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import com.squareup.sqldelight.FactorySpec.Companion.FACTORY_NAME
import com.squareup.sqldelight.model.Table
import com.squareup.sqldelight.model.adapterField
import com.squareup.sqldelight.model.isHandledType
import com.squareup.sqldelight.model.isNullable
import com.squareup.sqldelight.model.javaType
import com.squareup.sqldelight.validation.QueryResults
import org.antlr.v4.runtime.ParserRuleContext
import javax.lang.model.element.Modifier

internal class MapperSpec private constructor(private val nameAllocator: NameAllocator) {
  private val factoryFields = linkedSetOf(Table.CREATOR_FIELD)

  fun tableMapper(table: Table) = table.generateMapper()

  private fun Table.generateMapper(): TypeSpec.Builder {
    val mapper = TypeSpec.classBuilder(MAPPER_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .addSuperinterface(ParameterizedTypeName.get(MAPPER_TYPE, TypeVariableName.get("T")))
    val constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
    val factoryField = addFactoryField(mapper, constructor, "T", interfaceClassName)
    val mapReturn = CodeBlock.builder().add("$[return $factoryField.${Table.CREATOR_FIELD}.create(\n")

    column_def().forEachIndexed { i, column ->
      if (i != 0) mapReturn.add(",\n")
      mapReturn.add(column.cursorGetter(i, interfaceClassName))
    }

    val mapMethod = MethodSpec.methodBuilder("map")
        .addAnnotation(Override::class.java)
        .addModifiers(Modifier.PUBLIC)
        .returns(TypeVariableName.get("T"))
        .addParameter(ParameterSpec.builder(CURSOR_TYPE, CURSOR_PARAM)
            .addAnnotation(NONNULL_TYPE)
            .build())
        .addCode(mapReturn.add("$]\n);\n").build())

    return mapper.addMethod(constructor.build())
        .addMethod(mapMethod.build())
  }

  fun queryResultsMapper(queryResults: QueryResults) = queryResults.generateMapper()

  private fun QueryResults.generateMapper(): TypeSpec.Builder {
    val mapper = TypeSpec.classBuilder(mapperName)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .addTypeVariable(TypeVariableName.get("T", interfaceType))
        .addSuperinterface(ParameterizedTypeName.get(MAPPER_TYPE, TypeVariableName.get("T")))
        .addField(ParameterizedTypeName.get(creatorType, TypeVariableName.get("T")),
            Table.CREATOR_FIELD, Modifier.PRIVATE, Modifier.FINAL)

    val constructor = MethodSpec.constructorBuilder()
        .addModifiers(if (isView) Modifier.PUBLIC else Modifier.PRIVATE)
        .addParameter(ParameterizedTypeName.get(creatorType, TypeVariableName.get("T")),
            Table.CREATOR_FIELD)
        .addStatement("this.${Table.CREATOR_FIELD} = ${Table.CREATOR_FIELD}")

    val mapMethod = MethodSpec.methodBuilder("map")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override::class.java)
        .addAnnotation(MapperSpec.NONNULL_TYPE)
        .returns(TypeVariableName.get("T"))
        .addParameter(ParameterSpec.builder(CURSOR_TYPE, CURSOR_PARAM)
            .addAnnotation(NONNULL_TYPE)
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
      typeVariable: String,
      interfaceClass: ClassName
  ): String {
    val factoryFieldname = "${interfaceClass.simpleName().decapitalize()}$FACTORY_NAME"
    if (!factoryFields.add(factoryFieldname)) return factoryFieldname
    val factoryType = ParameterizedTypeName.get(interfaceClass.nestedClass(FACTORY_NAME),
        TypeVariableName.get(typeVariable))
    mapper.addField(factoryType, factoryFieldname, Modifier.PRIVATE, Modifier.FINAL)
    constructor.addParameter(factoryType, factoryFieldname)
        .addStatement("this.$factoryFieldname = $factoryFieldname")
    mapper.addTypeVariable(TypeVariableName.get(typeVariable, interfaceClass))
    return factoryFieldname
  }

  private fun QueryResults.cursorGetter(
      mapper: TypeSpec.Builder,
      constructor: MethodSpec.Builder,
      typePrefix: String = "",
      rootCreator: Boolean = false
  ): CodeBlock {
    val creatorField = if (rootCreator) Table.CREATOR_FIELD else "$queryName${Table.CREATOR_CLASS_NAME}"

    // For foreign tables we need to add their factory as a field so it can be used
    // during mapping. Note that values can also have foreign tables if they are
    // custom types grabbed from a foreign table.
    foreignTypes().forEachIndexed { i, foreignTable ->
      addFactoryField(mapper, constructor, "${typePrefix}R${i+1}", foreignTable)
    }

    if (isView && factoryFields.add(creatorField)) {
      // Add the parameters and fields to the mapper/constructor
      val type = ParameterizedTypeName.get(creatorType, TypeVariableName.get(typePrefix))
      mapper.addTypeVariable(TypeVariableName.get(typePrefix, interfaceType))
          .addField(type, creatorField, Modifier.PRIVATE, Modifier.FINAL)

      constructor.addParameter(type, creatorField)
          .addStatement("this.$creatorField = $creatorField")
    }

    val mapReturn = CodeBlock.builder().add("$creatorField.create(\n")
    mapReturn.indent().indent()
    var viewIndex = 1
    sortedResultsMap(
        { columnName, indexedValue -> indexedValue.cursorGetter() },
        { tableName, table -> table.cursorGetter() },
        { viewName, view -> view.cursorGetter(mapper, constructor, "${typePrefix}V${viewIndex++}") }
    ).forEachIndexed { i, codeBlock ->
      if (i != 0) mapReturn.add(",\n")
      mapReturn.add(codeBlock)
    }
    mapReturn.unindent().unindent()

    return mapReturn.add("\n)").build()
  }

  private fun QueryResults.IndexedValue.cursorGetter(): CodeBlock {
    val code = CodeBlock.builder()
    if (value.columnDef?.isNullable ?: true) {
      code.add("$CURSOR_PARAM.isNull($index) ? null : ")
    }
    if (value.columnDef?.isHandledType ?: true) {
      code.add(handledTypeGetter(javaType, index, value.element))
    } else {
      val factoryField = "${tableInterface!!.simpleName().decapitalize()}$FACTORY_NAME"
      code.add("$factoryField.${value.columnDef!!.adapterField(nameAllocator)}.$MAP_FUNCTION($CURSOR_PARAM, $index)")
    }
    return code.build()
  }

  private fun SqliteParser.Column_defContext.cursorGetter(index: Int, interfaceClass: ClassName): CodeBlock {
    val code = CodeBlock.builder()
    if (isNullable) {
      code.add("$CURSOR_PARAM.isNull($index) ? null : ")
    }
    if (isHandledType) {
      code.add(handledTypeGetter(javaType, index, this))
    } else {
      val factoryField = "${interfaceClass.simpleName().decapitalize()}$FACTORY_NAME"
      code.add("$factoryField.${adapterField(nameAllocator)}.$MAP_FUNCTION($CURSOR_PARAM, $index)")
    }
    return code.build()
  }

  private fun QueryResults.QueryTable.cursorGetter(): CodeBlock {
    val factoryField = "${interfaceType.simpleName().decapitalize()}$FACTORY_NAME"

    val code = CodeBlock.builder()
        .add("$factoryField.${Table.CREATOR_FIELD}.create(\n")
        .indent().indent()
    indexedValues.sortedBy { it.index }.map { it.cursorGetter() }.forEachIndexed { i, codeBlock ->
      if (i != 0) code.add(",\n")
      code.add(codeBlock)
    }

    return code.unindent().unindent().add("\n)").build()
  }

  companion object {
    private val NONNULL_TYPE = ClassName.get("android.support.annotation", "NonNull")
    internal val CURSOR_TYPE = ClassName.get("android.database", "Cursor")
    internal val CURSOR_PARAM = "cursor"
    internal val MAPPER_NAME = "Mapper"
    internal val MAPPER_TYPE = ClassName.get("com.squareup.sqldelight", "RowMapper")
    internal val MAP_FUNCTION = "map"

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
      throw SqlitePluginException(element, "Unknown cursor getter for type $javaType")
    }

    internal fun builder(
        nameAllocator: NameAllocator,
        queryResults: QueryResults
    ) = MapperSpec(nameAllocator).queryResultsMapper(queryResults)

    internal fun builder(table: Table) = MapperSpec(table.nameAllocator).tableMapper(table)
  }
}
