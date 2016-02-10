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
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import com.squareup.sqldelight.model.Column
import com.squareup.sqldelight.model.Column.Type.BOOLEAN
import com.squareup.sqldelight.model.Column.Type.ENUM
import com.squareup.sqldelight.model.Table
import java.util.LinkedHashMap
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PROTECTED
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC

class MarshalSpec(private val table: Table<*>) {
  internal fun build(): TypeSpec {
    val marshal = TypeSpec.classBuilder(table.marshalClassName.simpleName())
        .addModifiers(PUBLIC, STATIC)
        .addTypeVariable(TypeVariableName.get("T",
            ParameterizedTypeName.get(table.marshalClassName, TypeVariableName.get("T"))))

    if (table.isKeyValue) {
      marshal
          .addField(FieldSpec.builder(MAP_CLASS, CONTENTVALUES_MAP_FIELD, FINAL, PROTECTED)
              .initializer("new \$T<>()", LinkedHashMap::class.java)
              .build())
          .addMethod(MethodSpec.methodBuilder(CONTENTVALUES_METHOD)
              .addModifiers(PUBLIC, FINAL)
              .returns(ParameterizedTypeName.get(ClassName.get(Collection::class.java),
                  CONTENTVALUES_TYPE))
              .addStatement("return ${CONTENTVALUES_MAP_FIELD}.values()")
              .build())
    } else {
      marshal
          .addField(FieldSpec.builder(CONTENTVALUES_TYPE, CONTENTVALUES_FIELD, PROTECTED)
              .initializer("new \$T()", CONTENTVALUES_TYPE)
              .build())
          .addMethod(MethodSpec.methodBuilder(CONTENTVALUES_METHOD)
              .addModifiers(PUBLIC, FINAL)
              .returns(CONTENTVALUES_TYPE)
              .addStatement("return ${CONTENTVALUES_FIELD}")
              .build())
    }

    val constructor = MethodSpec.constructorBuilder().addModifiers(PUBLIC)

    for (column in table.columns) {
      if (column.isHandledType) {
        marshal.addMethod(marshalMethod(column))
      } else {
        val columnMarshalType = table.marshalClassName(column)
        marshal.addType(marshalInterface(column))
            .addField(columnMarshalType, column.marshalField(), PRIVATE, FINAL)
        constructor.addParameter(columnMarshalType, column.marshalField())
            .addStatement("this.${column.marshalField()} = ${column.marshalField()}")
        marshal.addMethod(contentValuesMethod(column)
            .addModifiers(PUBLIC)
            .returns(TypeVariableName.get("T"))
            .addParameter(column.javaType, column.methodName)
            .addStatement("${column.marshalField()}.marshal(${CONTENTVALUES_FIELD}, " +
                "${column.name()}, ${column.methodName})")
            .addStatement("return (T) this")
            .build())
      }
    }

    return marshal.addMethod(constructor.build()).build()
  }

  private fun contentValuesMethod(column: Column<*>) =
      if (table.isKeyValue) {
        val methodBuilder = MethodSpec.methodBuilder(column.methodName)
        if (!column.isNullable && !column.javaType.isPrimitive) {
          methodBuilder.beginControlFlow("if (${column.methodName} == null)")
              .addStatement("throw new NullPointerException(" +
                  "\"Cannot insert NULL value for NOT NULL column ${column.name}\")")
              .endControlFlow()
        }
        methodBuilder
            .addStatement("\$T ${CONTENTVALUES_FIELD} = " +
                "${CONTENTVALUES_MAP_FIELD}.get(${column.fieldName})", CONTENTVALUES_TYPE)
            .beginControlFlow("if (${CONTENTVALUES_FIELD} == null)")
            .addStatement("${CONTENTVALUES_FIELD} = new \$T()", CONTENTVALUES_TYPE)
            .addStatement("${CONTENTVALUES_FIELD}.put(\$S, ${column.fieldName})",
                SqliteCompiler.KEY_VALUE_KEY_COLUMN)
            .addStatement("${CONTENTVALUES_MAP_FIELD}.put(${column.fieldName}, ${CONTENTVALUES_FIELD})")
            .endControlFlow()
      } else MethodSpec.methodBuilder(column.methodName)

  private fun marshalInterface(column: Column<*>) =
      TypeSpec.interfaceBuilder(column.marshalName())
          .addModifiers(PUBLIC)
          .addMethod(MethodSpec.methodBuilder(MARSHAL_METHOD_NAME)
              .addParameter(CONTENTVALUES_TYPE, CONTENTVALUES_FIELD)
              .addParameter(String::class.java, COLUMN_NAME_PARAM)
              .addParameter(column.javaType, column.methodName)
              .returns(TypeName.VOID)
              .addModifiers(PUBLIC, ABSTRACT)
              .build())
          .build()

  private fun marshalMethod(column: Column<*>) =
      if (column.isNullable && (column.type == ENUM || column.type == BOOLEAN)) {
        contentValuesMethod(column)
            .beginControlFlow("if (${column.methodName} == null)")
            .addStatement("${CONTENTVALUES_FIELD}.putNull(${column.fieldName})")
            .addStatement("return (T) this")
            .endControlFlow()
      } else {
        contentValuesMethod(column)
      }
          .addModifiers(PUBLIC)
          .addParameter(column.javaType, column.methodName)
          .returns(TypeVariableName.get("T"))
          .addStatement("${CONTENTVALUES_FIELD}.put(${column.name()}, ${column.marshaledValue()})")
          .addStatement("return (T) this")
          .build()

  private fun Column<*>.name() = if (table.isKeyValue) VALUE else fieldName

  companion object {
    private val CONTENTVALUES_TYPE = ClassName.get("android.content", "ContentValues")
    private val CONTENTVALUES_FIELD = "contentValues"
    private val CONTENTVALUES_METHOD = "asContentValues"
    private val MARSHAL_METHOD_NAME = "marshal"
    private val COLUMN_NAME_PARAM = "columnName"
    private val CONTENTVALUES_MAP_FIELD = "contentValuesMap"
    private val MAP_CLASS = ParameterizedTypeName.get(ClassName.get(Map::class.java),
        ClassName.get(String::class.java),
        CONTENTVALUES_TYPE)
    private val VALUE = "\"" + SqliteCompiler.KEY_VALUE_VALUE_COLUMN + "\""

    internal fun builder(table: Table<*>) = MarshalSpec(table)
  }
}
