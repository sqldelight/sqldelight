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
package com.squareup.sqlite.android

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import com.squareup.sqlite.android.model.Column
import com.squareup.sqlite.android.model.Column.Type
import com.squareup.sqlite.android.model.Table
import java.util.LinkedHashMap
import javax.lang.model.element.Modifier
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PROTECTED
import javax.lang.model.element.Modifier.PUBLIC

class MarshalSpec(private val table: Table<*>) {
  internal fun build(): TypeSpec {
    val marshal = TypeSpec.classBuilder(table.marshalName())
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addTypeVariable(TypeVariableName.get("T", ClassName.get(table.packageName,
            "${table.interfaceName}.${table.marshalName()}")))

    if (table.isKeyValue) {
      marshal
          .addField(FieldSpec.builder(MAP_CLASS, CONTENTVALUES_MAP_FIELD, FINAL, PROTECTED)
              .initializer("new \$T<>()", ClassName.get(LinkedHashMap::class.java))
              .build())
          .addMethod(MethodSpec.methodBuilder(CONTENTVALUES_METHOD)
              .addModifiers(Modifier.PUBLIC, FINAL)
              .returns(ParameterizedTypeName.get(ClassName.get(Collection::class.java),
                  CONTENTVALUES_TYPE))
              .addStatement("return $CONTENTVALUES_MAP_FIELD.values()")
              .build())
    } else {
      marshal
          .addField(FieldSpec.builder(CONTENTVALUES_TYPE, CONTENTVALUES_FIELD, PROTECTED)
              .initializer("new \$T()", CONTENTVALUES_TYPE)
              .build())
          .addMethod(MethodSpec.methodBuilder(CONTENTVALUES_METHOD)
              .addModifiers(Modifier.PUBLIC, FINAL)
              .returns(CONTENTVALUES_TYPE)
              .addStatement("return $CONTENTVALUES_FIELD")
              .build())
    }

    val constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)

    for (column in table.columns) {
      if (column.isHandledType) {
        marshal.addMethod(marshalMethod(column))
      } else {
        val columnMarshalType = ClassName.get(table.packageName,
            "${table.interfaceName}.${table.marshalName()}.${column.marshalName()}")
        marshal.addType(marshalInterface(column))
            .addField(columnMarshalType, column.marshalField(), Modifier.PRIVATE, FINAL)
        constructor.addParameter(columnMarshalType, column.marshalField())
            .addStatement("this.${column.marshalField()} = ${column.marshalField()}")
        marshal.addMethod(contentValuesMethod(column)
            .addModifiers(Modifier.PUBLIC)
            .returns(TypeVariableName.get("T"))
            .addParameter(column.javaType, column.methodName)
            .addStatement("${column.marshalField()}.marshal($CONTENTVALUES_FIELD, " +
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
            .addStatement("\$T $CONTENTVALUES_FIELD = " +
                "$CONTENTVALUES_MAP_FIELD.get(${column.fieldName})", CONTENTVALUES_TYPE)
            .beginControlFlow("if ($CONTENTVALUES_FIELD == null)")
            .addStatement("$CONTENTVALUES_FIELD = new \$T()", CONTENTVALUES_TYPE)
            .addStatement("$CONTENTVALUES_FIELD.put(\$S, ${column.fieldName})",
                SqliteCompiler.KEY_VALUE_KEY_COLUMN)
            .addStatement("$CONTENTVALUES_MAP_FIELD.put(${column.fieldName}, $CONTENTVALUES_FIELD)")
            .endControlFlow()
      } else MethodSpec.methodBuilder(column.methodName)

  private fun marshalInterface(column: Column<*>) =
      TypeSpec.interfaceBuilder(column.marshalName())
          .addModifiers(PUBLIC)
          .addMethod(MethodSpec.methodBuilder(MARSHAL_METHOD_NAME)
              .addParameter(CONTENTVALUES_TYPE, CONTENTVALUES_FIELD)
              .addParameter(ClassName.get(String::class.java), COLUMN_NAME_PARAM)
              .addParameter(column.javaType, column.methodName)
              .returns(TypeName.VOID)
              .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
              .build())
          .build()

  private fun marshalMethod(column: Column<*>) =
      if (column.isNullable && (column.type == Type.ENUM || column.type == Type.BOOLEAN)) {
        contentValuesMethod(column)
            .beginControlFlow("if (${column.methodName} == null)")
            .addStatement("$CONTENTVALUES_FIELD.putNull(${column.fieldName})")
            .addStatement("return (T) this")
            .endControlFlow()
      } else {
        contentValuesMethod(column)
      }
          .addModifiers(Modifier.PUBLIC)
          .addParameter(column.javaType, column.methodName)
          .returns(TypeVariableName.get("T"))
          .addStatement("$CONTENTVALUES_FIELD.put(${column.name()}, ${column.marshaledValue()})")
          .addStatement("return (T) this")
          .build()

  private fun Table<*>.marshalName() = name + "Marshal"
  private fun Column<*>.name() = if (table.isKeyValue) VALUE else fieldName
  private fun Column<*>.marshalName() = Column.marshalName(name)
  private fun Column<*>.marshalField() = Column.marshalField(name)
  private fun Column<*>.marshaledValue() =
      when (type) {
        Column.Type.INT, Column.Type.LONG, Column.Type.SHORT, Column.Type.DOUBLE, Column.Type.FLOAT,
        Column.Type.STRING, Column.Type.BLOB -> methodName
        Column.Type.BOOLEAN -> "$methodName ? 1 : 0"
        Column.Type.ENUM -> "$methodName.name()"
        else -> throw IllegalStateException("Unexpected type")
      }

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
