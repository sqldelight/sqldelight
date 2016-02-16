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
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import com.squareup.sqldelight.model.Column
import com.squareup.sqldelight.model.Column.Type.BOOLEAN
import com.squareup.sqldelight.model.Column.Type.ENUM
import com.squareup.sqldelight.model.Table
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

    marshal
        .addField(FieldSpec.builder(CONTENTVALUES_TYPE, CONTENTVALUES_FIELD, PROTECTED)
            .initializer("new \$T()", CONTENTVALUES_TYPE)
            .build())
        .addMethod(MethodSpec.methodBuilder(CONTENTVALUES_METHOD)
            .addModifiers(PUBLIC, FINAL)
            .returns(CONTENTVALUES_TYPE)
            .addStatement("return $CONTENTVALUES_FIELD")
            .build())

    val constructor = MethodSpec.constructorBuilder().addModifiers(PUBLIC)

    for (column in table.columns) {
      if (column.isHandledType) {
        marshal.addMethod(marshalMethod(column))
      } else {
        marshal.addField(column.adapterType(), column.adapterField(), PRIVATE, FINAL)
        constructor.addParameter(column.adapterType(), column.adapterField())
            .addStatement("this.${column.adapterField()} = ${column.adapterField()}")
        marshal.addMethod(contentValuesMethod(column)
            .addModifiers(PUBLIC)
            .returns(TypeVariableName.get("T"))
            .addParameter(column.javaType, column.methodName)
            .addStatement("${column.adapterField()}.marshal($CONTENTVALUES_FIELD, " +
                "${column.constantName}, ${column.methodName})")
            .addStatement("return (T) this")
            .build())
      }
    }

    return marshal.addMethod(constructor.build()).build()
  }

  private fun contentValuesMethod(column: Column<*>) = MethodSpec.methodBuilder(column.methodName)

  private fun marshalMethod(column: Column<*>) =
      if (column.isNullable && (column.type == ENUM || column.type == BOOLEAN)) {
        contentValuesMethod(column)
            .beginControlFlow("if (${column.methodName} == null)")
            .addStatement("$CONTENTVALUES_FIELD.putNull(${column.constantName})")
            .addStatement("return (T) this")
            .endControlFlow()
      } else {
        contentValuesMethod(column)
      }
          .addModifiers(PUBLIC)
          .addParameter(column.javaType, column.methodName)
          .returns(TypeVariableName.get("T"))
          .addStatement("$CONTENTVALUES_FIELD.put(${column.constantName}, ${column.marshaledValue()})")
          .addStatement("return (T) this")
          .build()

  companion object {
    private val CONTENTVALUES_TYPE = ClassName.get("android.content", "ContentValues")
    private val CONTENTVALUES_FIELD = "contentValues"
    private val CONTENTVALUES_METHOD = "asContentValues"

    internal fun builder(table: Table<*>) = MarshalSpec(table)
  }
}
