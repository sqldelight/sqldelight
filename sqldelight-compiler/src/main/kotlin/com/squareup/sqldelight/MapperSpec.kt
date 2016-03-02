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
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import com.squareup.sqldelight.model.Column
import com.squareup.sqldelight.model.Column.Type.BLOB
import com.squareup.sqldelight.model.Column.Type.BOOLEAN
import com.squareup.sqldelight.model.Column.Type.DOUBLE
import com.squareup.sqldelight.model.Column.Type.ENUM
import com.squareup.sqldelight.model.Column.Type.FLOAT
import com.squareup.sqldelight.model.Column.Type.INT
import com.squareup.sqldelight.model.Column.Type.LONG
import com.squareup.sqldelight.model.Column.Type.SHORT
import com.squareup.sqldelight.model.Column.Type.STRING
import com.squareup.sqldelight.model.Table
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PROTECTED
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC

class MapperSpec private constructor(private val table: Table) {
  private val creatorType = ParameterizedTypeName.get(table.creatorClassName,
      TypeVariableName.get("T"))

  fun build(): TypeSpec {
    val mapper = TypeSpec.classBuilder(table.mapperClassName.simpleName())
        .addTypeVariable(TypeVariableName.get("T", table.interfaceClassName))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addField(creatorType, CREATOR_FIELD, PRIVATE, FINAL)

    mapper.addType(creatorInterface())

    for (column in table.columns) {
      if (column.isHandledType) continue;
      mapper.addField(column.adapterType(), column.adapterField(), PRIVATE, FINAL)
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

    for (column in table.columns) {
      if (!column.isHandledType) {
        constructor.addParameter(column.adapterType(), column.adapterField())
            .addStatement("this.${column.adapterField()} = ${column.adapterField()}")
      }
    }

    return constructor.build()
  }

  private fun mapperMethod(): MethodSpec {
    val mapReturn = CodeBlock.builder().add("$[return $CREATOR_FIELD.create(\n")

    for (column in table.columns) {
      if (column != table.columns[0]) mapReturn.add(",\n")
      if (column.isHandledType) {
        mapReturn.add(cursorMapper(column))
      } else {
        if (column.isNullable) {
          mapReturn.add("$CURSOR_PARAM.isNull(" +
              "$CURSOR_PARAM.getColumnIndex(${column.constantName})) ? null : ")
        }
        mapReturn.add("${column.adapterField()}.$MAP_FUNCTION(" +
            "$CURSOR_PARAM, $CURSOR_PARAM.getColumnIndex(${column.constantName}))")
      }
    }

    return MethodSpec.methodBuilder(MAP_FUNCTION)
        .addModifiers(PUBLIC)
        .returns(TypeVariableName.get("T"))
        .addParameter(CURSOR_TYPE, CURSOR_PARAM)
        .addCode(mapReturn.add("$]\n);\n").build())
        .build()
  }

  private fun cursorMapper(column: Column, columnName: String = column.constantName): CodeBlock {
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

    for (column in table.columns) {
      create.addParameter(column.javaType, column.methodName)
    }

    return TypeSpec.interfaceBuilder(table.creatorClassName.simpleName())
        .addTypeVariable(TypeVariableName.get("R", table.interfaceClassName))
        .addModifiers(PUBLIC)
        .addMethod(create.build())
        .build()
  }

  private fun Column.cursorGetter(getter: String) =
      when (type) {
        ENUM -> CodeBlock.builder().add(
            "\$T.valueOf($CURSOR_PARAM.getString($getter))", javaType).build()
        INT -> CodeBlock.builder().add("$CURSOR_PARAM.getInt($getter)").build()
        LONG -> CodeBlock.builder().add("$CURSOR_PARAM.getLong($getter)").build()
        SHORT -> CodeBlock.builder().add("$CURSOR_PARAM.getShort($getter)").build()
        DOUBLE -> CodeBlock.builder().add("$CURSOR_PARAM.getDouble($getter)").build()
        FLOAT -> CodeBlock.builder().add("$CURSOR_PARAM.getFloat($getter)").build()
        BOOLEAN -> CodeBlock.builder().add("$CURSOR_PARAM.getInt($getter) == 1").build()
        BLOB -> CodeBlock.builder().add("$CURSOR_PARAM.getBlob($getter)").build()
        STRING -> CodeBlock.builder().add("$CURSOR_PARAM.getString($getter)").build()
        else -> throw SqlitePluginException(originatingElement,
            "Unknown cursor getter for type $javaType")
      }

  companion object {
    private val CREATOR_FIELD = "creator"
    private val CREATOR_METHOD_NAME = "create"
    private val CURSOR_TYPE = ClassName.get("android.database", "Cursor")
    private val CURSOR_PARAM = "cursor"
    private val MAP_FUNCTION = "map"

    fun builder(table: Table) = MapperSpec(table)
  }
}
