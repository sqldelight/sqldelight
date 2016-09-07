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
import com.squareup.javapoet.MethodSpec.Builder
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.sqldelight.resolution.query.Table
import com.squareup.sqldelight.resolution.query.Value
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PROTECTED
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC

internal class MarshalSpec(private val table: Table) {
  private val marshalClassName = table.javaType.nestedClass("Marshal")

  internal fun build(): TypeSpec {
    val marshal = TypeSpec.classBuilder(marshalClassName.simpleName())
        .addModifiers(PUBLIC, STATIC, FINAL)

    marshal
        .addField(FieldSpec.builder(CONTENTVALUES_TYPE, CONTENTVALUES_FIELD, PROTECTED, FINAL)
            .initializer("new \$T()", CONTENTVALUES_TYPE)
            .build())
        .addMethod(MethodSpec.methodBuilder(CONTENTVALUES_METHOD)
            .addModifiers(PUBLIC)
            .returns(CONTENTVALUES_TYPE)
            .addStatement("return $CONTENTVALUES_FIELD")
            .build())

    val copyConstructor = MethodSpec.constructorBuilder()
    copyConstructor.addParameter(ParameterSpec.builder(table.javaType, "copy")
        .addAnnotation(SqliteCompiler.NULLABLE)
        .build())

    for (column in table.columns) {
      if (column.isHandledType) {
        marshal.addMethod(marshalMethod(column))
      } else {
        marshal.addField(column.adapterType, column.adapterField, PRIVATE, FINAL)

        copyConstructor.addParameter(column.adapterType, column.adapterField)
            .addStatement("this.${column.adapterField} = ${column.adapterField}")

        val marshalMethod = contentValuesMethod(column)
            .addModifiers(PUBLIC)
            .returns(marshalClassName)

        val parameter = ParameterSpec.builder(column.javaType, column.paramName)
        if (column.nullable) {
          parameter.addAnnotation(SqliteCompiler.NULLABLE)
        } else if (!column.javaType.isPrimitive) {
          parameter.addAnnotation(SqliteCompiler.NON_NULL)
        }
        marshalMethod.addParameter(parameter.build())

        if (column.nullable) {
          marshalMethod.beginControlFlow("if (${column.paramName} != null)")
        }
        marshalMethod.addStatement("$CONTENTVALUES_FIELD.put(${column.constantName}, " +
                "${column.adapterField}.encode(${column.paramName}))")
        if (column.nullable) {
          marshalMethod.nextControlFlow("else")
              .addStatement("$CONTENTVALUES_FIELD.putNull(${column.constantName})")
              .endControlFlow()
        }
        marshalMethod.addStatement("return this")

        marshal.addMethod(marshalMethod.build())
      }
    }
    copyConstructor.beginControlFlow("if (copy != null)")
    for (column in table.columns) {
      val methodName = column.methodName
      copyConstructor.addStatement("this.$methodName(copy.$methodName())")
    }
    copyConstructor.endControlFlow()

    return marshal.addMethod(copyConstructor.build()).build()
  }

  private fun contentValuesMethod(column: Value) : Builder {
    val builder = MethodSpec.methodBuilder(column.methodName)
    if (column.javadocText != null) builder.addJavadoc(column.javadocText)
    return builder
  }

  private fun marshalMethod(column: Value) =
      if (column.nullable && column.javaType == TypeName.BOOLEAN.box()) {
        contentValuesMethod(column)
            .beginControlFlow("if (${column.paramName} == null)")
            .addStatement("$CONTENTVALUES_FIELD.putNull(${column.constantName})")
            .addStatement("return this")
            .endControlFlow()
      } else {
        contentValuesMethod(column)
      }
          .addModifiers(PUBLIC)
          .addParameter(column.javaType, column.paramName)
          .returns(marshalClassName)
          .addStatement(
              "$CONTENTVALUES_FIELD.put(${column.constantName}, ${column.marshaledValue()})")
          .addStatement("return this")
          .build()

  companion object {
    private val CONTENTVALUES_TYPE = ClassName.get("android.content", "ContentValues")
    private val CONTENTVALUES_FIELD = "contentValues"
    private val CONTENTVALUES_METHOD = "asContentValues"

    internal fun builder(table: Table) = MarshalSpec(table)
  }
}
