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
import com.squareup.sqldelight.model.adapterField
import com.squareup.sqldelight.model.adapterType
import com.squareup.sqldelight.model.constantName
import com.squareup.sqldelight.model.isHandledType
import com.squareup.sqldelight.model.isNullable
import com.squareup.sqldelight.model.javaType
import com.squareup.sqldelight.model.methodName
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PROTECTED
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC

class MapperSpec private constructor(
    private val table: SqliteParser.Create_table_stmtContext,
    private val interfaceClassName: ClassName,
    private val nameAllocator: NameAllocator
) {
  private val mapperClassName = interfaceClassName.nestedClass("Mapper")
  private val creatorClassName = mapperClassName.nestedClass("Creator")
  private val creatorType = ParameterizedTypeName.get(creatorClassName, TypeVariableName.get("T"))

  fun build(): TypeSpec {
    val mapper = TypeSpec.classBuilder(mapperClassName.simpleName())
        .addTypeVariable(TypeVariableName.get("T", interfaceClassName))
        .addSuperinterface(ParameterizedTypeName.get(MAPPER_TYPE, TypeVariableName.get("T")))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addField(creatorType, CREATOR_FIELD, PRIVATE, FINAL)

    mapper.addType(creatorInterface())

    for (column in table.column_def()) {
      if (column.isHandledType) continue;
      mapper.addField(column.adapterType(), column.adapterField(nameAllocator), PRIVATE, FINAL)
    }

    return mapper
        .addMethod(constructor())
        .addMethod(mapperMethod())
        .build()
  }

  private fun constructor(): MethodSpec {
    val constructor = MethodSpec.constructorBuilder()
        .addModifiers(PROTECTED)
        .addParameter(creatorType, CREATOR_FIELD)
        .addStatement("this.$CREATOR_FIELD = $CREATOR_FIELD")

    for (column in table.column_def()) {
      if (!column.isHandledType) {
        constructor.addParameter(column.adapterType(), column.adapterField(nameAllocator))
            .addStatement("this.${column.adapterField(nameAllocator)} = ${column.adapterField(nameAllocator)}")
      }
    }

    return constructor.build()
  }

  private fun mapperMethod(): MethodSpec {
    val mapReturn = CodeBlock.builder().add("$[return $CREATOR_FIELD.create(\n")

    for (column in table.column_def()) {
      if (column != table.column_def(0)) mapReturn.add(",\n")
      if (column.isHandledType) {
        mapReturn.add(cursorMapper(column))
      } else {
        if (column.isNullable) {
          mapReturn.add("$CURSOR_PARAM.isNull(" +
              "$CURSOR_PARAM.getColumnIndex(${column.constantName(nameAllocator)})) ? null : ")
        }
        mapReturn.add("${column.adapterField(nameAllocator)}.$MAP_FUNCTION(" +
            "$CURSOR_PARAM, $CURSOR_PARAM.getColumnIndex(${column.constantName(nameAllocator)}))")
      }
    }

    return MethodSpec.methodBuilder(MAP_FUNCTION)
        .addAnnotation(Override::class.java)
        .addAnnotation(NONNULL_TYPE)
        .addModifiers(PUBLIC)
        .returns(TypeVariableName.get("T"))
        .addParameter(ParameterSpec.builder(CURSOR_TYPE, CURSOR_PARAM)
            .addAnnotation(NONNULL_TYPE)
            .build())
        .addCode(mapReturn.add("$]\n);\n").build())
        .build()
  }

  private fun cursorMapper(
      column: SqliteParser.Column_defContext,
      columnName: String = column.constantName(nameAllocator)
  ): CodeBlock {
    val code = CodeBlock.builder()
    if (column.isNullable) {
      code.add("$CURSOR_PARAM.isNull($CURSOR_PARAM.getColumnIndex($columnName)) ? null : ")
    }
    return code.add(column.cursorGetter("$CURSOR_PARAM.getColumnIndex($columnName)")).build()
  }

  private fun creatorInterface(): TypeSpec {
    val create = MethodSpec.methodBuilder(CREATOR_METHOD_NAME)
        .returns(TypeVariableName.get("R"))
        .addModifiers(PUBLIC, ABSTRACT)

    for (column in table.column_def()) {
      create.addParameter(column.javaType, column.methodName(nameAllocator))
    }

    return TypeSpec.interfaceBuilder(creatorClassName.simpleName())
        .addTypeVariable(TypeVariableName.get("R", interfaceClassName))
        .addModifiers(PUBLIC)
        .addMethod(create.build())
        .build()
  }

  private fun SqliteParser.Column_defContext.cursorGetter(getter: String): CodeBlock {
    if (javaType == TypeName.BOOLEAN || javaType == TypeName.BOOLEAN.box()) {
      return CodeBlock.builder().add("$CURSOR_PARAM.getInt($getter) == 1").build()
    }
    if (javaType == TypeName.INT || javaType == TypeName.INT.box()) {
      return CodeBlock.builder().add("$CURSOR_PARAM.getInt($getter)").build()
    }
    if (javaType == TypeName.LONG || javaType == TypeName.LONG.box()) {
      return CodeBlock.builder().add("$CURSOR_PARAM.getLong($getter)").build()
    }
    if (javaType == TypeName.FLOAT || javaType == TypeName.FLOAT.box()) {
      return CodeBlock.builder().add("$CURSOR_PARAM.getFloat($getter)").build()
    }
    if (javaType == TypeName.DOUBLE || javaType == TypeName.DOUBLE.box()) {
      return CodeBlock.builder().add("$CURSOR_PARAM.getDouble($getter)").build()
    }
    if (javaType == ArrayTypeName.of(TypeName.BYTE)) {
      return CodeBlock.builder().add("$CURSOR_PARAM.getBlob($getter)").build()
    }
    if (javaType == ClassName.get(String::class.java)) {
      return CodeBlock.builder().add("$CURSOR_PARAM.getString($getter)").build()
    }
    throw SqlitePluginException(this, "Unknown cursor getter for type $javaType")
  }

  companion object {
    private val CREATOR_FIELD = "creator"
    private val CREATOR_METHOD_NAME = "create"
    private val CURSOR_TYPE = ClassName.get("android.database", "Cursor")
    private val CURSOR_PARAM = "cursor"
    private val MAPPER_TYPE = ClassName.get("com.squareup.sqldelight", "RowMapper")
    private val MAP_FUNCTION = "map"
    private val NONNULL_TYPE = ClassName.get("android.support.annotation", "NonNull")

    fun builder(
        table: SqliteParser.Create_table_stmtContext,
        interfaceClassName: ClassName,
        nameAllocator: NameAllocator
    ) = MapperSpec(table, interfaceClassName, nameAllocator)
  }
}
