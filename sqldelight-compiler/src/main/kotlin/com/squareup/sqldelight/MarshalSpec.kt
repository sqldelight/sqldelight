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
import com.squareup.sqldelight.model.Table
import com.squareup.sqldelight.model.adapterField
import com.squareup.sqldelight.model.adapterType
import com.squareup.sqldelight.model.constantName
import com.squareup.sqldelight.model.isHandledType
import com.squareup.sqldelight.model.isNullable
import com.squareup.sqldelight.model.javaType
import com.squareup.sqldelight.model.marshaledValue
import com.squareup.sqldelight.model.methodName
import com.squareup.sqldelight.model.paramName
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PROTECTED
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC

internal class MarshalSpec(private val table: Table) {
  private val marshalClassName = table.interfaceClassName.nestedClass("Marshal")
  private val nameAllocator = table.nameAllocator

  internal fun build(): TypeSpec {
    val marshal = TypeSpec.classBuilder(marshalClassName.simpleName())
        .addModifiers(PUBLIC, STATIC)
        .addTypeVariable(TypeVariableName.get("T",
            ParameterizedTypeName.get(marshalClassName, TypeVariableName.get("T"))))

    marshal
        .addField(FieldSpec.builder(CONTENTVALUES_TYPE, CONTENTVALUES_FIELD, PROTECTED)
            .initializer("new \$T()", CONTENTVALUES_TYPE)
            .build())
        .addMethod(MethodSpec.methodBuilder(CONTENTVALUES_METHOD)
            .addModifiers(PUBLIC, FINAL)
            .returns(CONTENTVALUES_TYPE)
            .addStatement("return $CONTENTVALUES_FIELD")
            .build())

    val copyConstructor = MethodSpec.constructorBuilder().addModifiers(PUBLIC)
    copyConstructor.addParameter(table.interfaceClassName, "copy");

    val constructor = MethodSpec.constructorBuilder().addModifiers(PUBLIC)

    for (column in table.column_def()) {
      if (column.isHandledType) {
        marshal.addMethod(marshalMethod(column))
      } else {
        marshal.addField(column.adapterType(), column.adapterField(nameAllocator), PRIVATE, FINAL)
        constructor.addParameter(column.adapterType(), column.adapterField(nameAllocator))
            .addStatement("this.${column.adapterField(nameAllocator)} = ${column.adapterField(nameAllocator)}")
        copyConstructor.addParameter(column.adapterType(), column.adapterField(nameAllocator))
            .addStatement("this.${column.adapterField(nameAllocator)} = ${column.adapterField(nameAllocator)}")
        marshal.addMethod(contentValuesMethod(column)
            .addModifiers(PUBLIC)
            .returns(TypeVariableName.get("T"))
            .addParameter(column.javaType, column.paramName(nameAllocator))
            .addStatement("${column.adapterField(nameAllocator)}.marshal($CONTENTVALUES_FIELD, " +
                "${column.constantName(nameAllocator)}, ${column.paramName(nameAllocator)})")
            .addStatement("return (T) this")
            .build())
      }
      copyConstructor.addStatement("this.${column.methodName(nameAllocator)}" +
          "(copy.${column.methodName(nameAllocator)}())")
    }

    return marshal.addMethod(constructor.build()).addMethod(copyConstructor.build()).build()
  }

  private fun contentValuesMethod(column: SqliteParser.Column_defContext)
      = MethodSpec.methodBuilder(column.methodName(nameAllocator))

  private fun marshalMethod(column: SqliteParser.Column_defContext) =
      if (column.isNullable && column.javaType == TypeName.BOOLEAN.box()) {
        contentValuesMethod(column)
            .beginControlFlow("if (${column.paramName(nameAllocator)} == null)")
            .addStatement("$CONTENTVALUES_FIELD.putNull(${column.constantName(nameAllocator)})")
            .addStatement("return (T) this")
            .endControlFlow()
      } else {
        contentValuesMethod(column)
      }
          .addModifiers(PUBLIC)
          .addParameter(column.javaType, column.paramName(nameAllocator))
          .returns(TypeVariableName.get("T"))
          .addStatement(
              "$CONTENTVALUES_FIELD.put(${column.constantName(nameAllocator)}," +
                  " ${column.marshaledValue(nameAllocator)})")
          .addStatement("return (T) this")
          .build()

  companion object {
    private val CONTENTVALUES_TYPE = ClassName.get("android.content", "ContentValues")
    private val CONTENTVALUES_FIELD = "contentValues"
    private val CONTENTVALUES_METHOD = "asContentValues"

    internal fun builder(table: Table) = MarshalSpec(table)
  }
}
