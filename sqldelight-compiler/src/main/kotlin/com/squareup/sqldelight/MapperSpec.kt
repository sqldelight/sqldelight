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
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import com.squareup.sqldelight.SqliteCompiler.Companion
import com.squareup.sqldelight.model.Column
import com.squareup.sqldelight.model.Column.Type.BLOB
import com.squareup.sqldelight.model.Column.Type.BOOLEAN
import com.squareup.sqldelight.model.Column.Type.CLASS
import com.squareup.sqldelight.model.Column.Type.DOUBLE
import com.squareup.sqldelight.model.Column.Type.ENUM
import com.squareup.sqldelight.model.Column.Type.FLOAT
import com.squareup.sqldelight.model.Column.Type.INT
import com.squareup.sqldelight.model.Column.Type.LONG
import com.squareup.sqldelight.model.Column.Type.SHORT
import com.squareup.sqldelight.model.Column.Type.STRING
import com.squareup.sqldelight.model.Table
import java.util.ArrayList
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PROTECTED
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC

class MapperSpec private constructor(private val table: Table<*>) {
  private val creatorType = ParameterizedTypeName.get(ClassName.get(table.packageName,
      "${table.interfaceName}.${MAPPER_NAME}.${CREATOR_TYPE_NAME}"),
      TypeVariableName.get("T"))

  fun build(): TypeSpec {
    val mapper = TypeSpec.classBuilder(MAPPER_NAME)
        .addTypeVariable(TypeVariableName.get("T", table.interfaceType))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addField(creatorType, CREATOR_FIELD, PRIVATE, FINAL)

    mapper.addType(creatorInterface())

    for (column in table.columns) {
      if (column.isHandledType) continue;
      val columnMapperType = ClassName.get(table.packageName,
          "${table.interfaceName}.${MAPPER_NAME}.${column.mapperName()}")
      mapper.addType(mapperInterface(column))
          .addField(columnMapperType, column.mapperField(), PRIVATE, FINAL)
    }

    return mapper
        .addMethod(constructor())
        .addMethod(if (table.isKeyValue) keyValueMapperMethod() else mapperMethod())
        .build()
  }

  private fun constructor(): MethodSpec {
    val constructor = MethodSpec.constructorBuilder()
        .addModifiers(PROTECTED)
        .addParameter(creatorType, CREATOR_FIELD)
        .addStatement("this.${CREATOR_FIELD} = ${CREATOR_FIELD}")

    for (column in table.columns) {
      if (!column.isHandledType) {
        val columnMapperType = ClassName.get(table.packageName,
            "${table.interfaceName}.${MAPPER_NAME}.${column.mapperName()}")
        constructor.addParameter(columnMapperType, column.mapperField())
            .addStatement("this.${column.mapperField()} = ${column.mapperField()}")
      }
    }

    return constructor.build()
  }

  private fun mapperMethod(): MethodSpec {
    val mapReturn = CodeBlock.builder().add("$[return ${CREATOR_FIELD}.create(\n")

    for (column in table.columns) {
      if (column != table.columns[0]) mapReturn.add(",\n")
      if (column.isHandledType) {
        mapReturn.add(cursorMapper(column))
      } else {
        if (column.isNullable) {
          mapReturn.add("${CURSOR_PARAM}.isNull(" +
              "${CURSOR_PARAM}.getColumnIndex(${column.fieldName})) ? null : ")
        }
        mapReturn.add("${column.mapperField()}.${MAP_FUNCTION}(" +
            "${CURSOR_PARAM}, ${CURSOR_PARAM}.getColumnIndex(${column.fieldName}))")
      }
    }

    return MethodSpec.methodBuilder(MAP_FUNCTION)
        .addModifiers(PUBLIC)
        .returns(TypeVariableName.get("T"))
        .addParameter(CURSOR_TYPE, CURSOR_PARAM)
        .addCode(mapReturn.add("$]\n);\n").build())
        .build()
  }

  private fun keyValueMapperMethod(): MethodSpec {
    val codeBlock = CodeBlock.builder()
    for (column in table.columns) {
      codeBlock.addStatement("\$T ${column.methodName} = ${DEFAULTS_PARAM} == null " +
          "? ${column.defaultValue()} " +
          ": ${DEFAULTS_PARAM}.${column.methodName}()", column.javaType)
    }
    codeBlock.beginControlFlow("try")
        .beginControlFlow("while (${CURSOR_PARAM}.moveToNext())")
        .addStatement("String key = cursor.getString(cursor.getColumnIndexOrThrow(\$S))",
            SqliteCompiler.KEY_VALUE_KEY_COLUMN)
        .beginControlFlow("switch (key)")

    val methodNames = ArrayList<String>()
    for (column in table.columns) {
      codeBlock.add("case ${column.fieldName}:\n")
          .indent()
          .add("${column.methodName} = ")

      if (column.isHandledType) {
        codeBlock.add(cursorMapper(column, "\"${SqliteCompiler.KEY_VALUE_VALUE_COLUMN}\""))
      } else {
        if (column.isNullable) {
          codeBlock.add("${CURSOR_PARAM}.isNull(${CURSOR_PARAM}.getColumnIndex(\$S)) ? null : ",
              SqliteCompiler.KEY_VALUE_VALUE_COLUMN)
        }
        codeBlock.add("${column.mapperField()}.${MAP_FUNCTION}(${CURSOR_PARAM}, " +
            "${CURSOR_PARAM}.getColumnIndex(\$S))", SqliteCompiler.KEY_VALUE_VALUE_COLUMN)
      }
      // Javapoet wants to put the break four spaces over, so we first have to unindent twice.
      codeBlock.unindent().unindent().addStatement(";\nbreak").indent()

      methodNames.add(column.methodName)
    }

    codeBlock.endControlFlow()
        .endControlFlow()
        .addStatement("return ${CREATOR_FIELD}.${CREATOR_METHOD_NAME}(" +
            "${table.columns.map({ it.methodName }).joinToString(",\n")})")
        .nextControlFlow("finally")
        .addStatement("cursor.close()")
        .endControlFlow()

    return MethodSpec.methodBuilder("map")
        .addModifiers(PUBLIC)
        .returns(TypeVariableName.get("T"))
        .addParameter(CURSOR_TYPE, CURSOR_PARAM)
        .addParameter(table.interfaceType, DEFAULTS_PARAM)
        .addCode(codeBlock.build())
        .build()
  }

  private fun cursorMapper(column: Column<*>, columnName: String = column.fieldName): CodeBlock {
    val code = CodeBlock.builder()
    if (column.isNullable) {
      code.add("${CURSOR_PARAM}.isNull(${CURSOR_PARAM}.getColumnIndex($columnName)) ? null : ")
    }
    return code.add(column.cursorGetter("${CURSOR_PARAM}.getColumnIndex($columnName)")).build()
  }

  private fun mapperInterface(column: Column<*>) =
      TypeSpec.interfaceBuilder(column.mapperName())
          .addModifiers(PUBLIC)
          .addMethod(MethodSpec.methodBuilder(MAP_FUNCTION)
              .returns(column.javaType)
              .addModifiers(PUBLIC, ABSTRACT)
              .addParameter(CURSOR_TYPE, CURSOR_PARAM)
              .addParameter(TypeName.INT, COLUMN_INDEX_PARAM)
              .build())
          .build()

  private fun creatorInterface(): TypeSpec {
    val create = MethodSpec.methodBuilder(CREATOR_METHOD_NAME)
        .returns(TypeVariableName.get("R"))
        .addModifiers(PUBLIC, ABSTRACT)

    for (column in table.columns) {
      create.addParameter(column.javaType, column.methodName)
    }

    return TypeSpec.interfaceBuilder(CREATOR_TYPE_NAME)
        .addTypeVariable(TypeVariableName.get("R", table.interfaceType))
        .addModifiers(PUBLIC)
        .addMethod(create.build())
        .build()
  }

  private fun Column<*>.mapperName() = Column.mapperName(name)
  private fun Column<*>.mapperField() = Column.mapperField(name)
  private fun Column<*>.defaultValue() =
      if (isNullable) "null"
      else when (type) {
        ENUM, STRING, CLASS, BLOB -> "null"
        INT, SHORT, LONG, DOUBLE, FLOAT -> "0"
        BOOLEAN -> "false"
        else -> throw SqlitePluginException(originatingElement as Any, "Unknown type " + type)
      }

  private fun Column<*>.cursorGetter(getter: String) =
      when (type) {
        ENUM -> CodeBlock.builder().add(
            "\$T.valueOf(${CURSOR_PARAM}.getString($getter))", javaType).build()
        INT -> CodeBlock.builder().add("${CURSOR_PARAM}.getInt($getter)").build()
        LONG -> CodeBlock.builder().add("${CURSOR_PARAM}.getLong($getter)").build()
        SHORT -> CodeBlock.builder().add("${CURSOR_PARAM}.getShort($getter)").build()
        DOUBLE -> CodeBlock.builder().add("${CURSOR_PARAM}.getDouble($getter)").build()
        FLOAT -> CodeBlock.builder().add("${CURSOR_PARAM}.getFloat($getter)").build()
        BOOLEAN -> CodeBlock.builder().add("${CURSOR_PARAM}.getInt($getter) == 1").build()
        BLOB -> CodeBlock.builder().add("${CURSOR_PARAM}.getBlob($getter)").build()
        STRING -> CodeBlock.builder().add("${CURSOR_PARAM}.getString($getter)").build()
        else -> throw SqlitePluginException(originatingElement as Any,
            "Unknown cursor getter for type $javaType")
      }

  companion object {
    private val CREATOR_TYPE_NAME = "Creator"
    private val CREATOR_FIELD = "creator"
    private val CREATOR_METHOD_NAME = "create"
    private val CURSOR_TYPE = ClassName.get("android.database", "Cursor")
    private val CURSOR_PARAM = "cursor"
    private val COLUMN_INDEX_PARAM = "columnIndex"
    private val MAP_FUNCTION = "map"
    private val DEFAULTS_PARAM = "defaults"
    private val MAPPER_NAME = "Mapper"

    fun builder(table: Table<*>) = MapperSpec(table)
  }
}
