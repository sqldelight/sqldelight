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
package com.squareup.sqldelight.model

import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.sqldelight.SqliteCompiler
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.SqlitePluginException

internal enum class Type(val defaultType: TypeName, val handledTypes: Set<TypeName>) {
  INTEGER(TypeName.LONG, setOf(TypeName.BOOLEAN.box(), TypeName.INT.box(), TypeName.LONG.box())),
  REAL(TypeName.DOUBLE, setOf(TypeName.FLOAT.box(), TypeName.DOUBLE.box())),
  TEXT(ClassName.get(String::class.java), setOf(ClassName.get(String::class.java))),
  BLOB(ArrayTypeName.of(TypeName.BYTE), setOf(ArrayTypeName.of(TypeName.BYTE)))
}

internal val SqliteParser.Column_defContext.name: String
  get() = column_name().text

internal val SqliteParser.Column_defContext.constantName: String
  get() = SqliteCompiler.constantName(name)

internal val SqliteParser.Column_defContext.methodName: String
  get() = methodName(name)

internal val SqliteParser.Column_defContext.type: Type
  get() = Type.valueOf(type_name().getChild(0).getChild(0).text)

internal val SqliteParser.Column_defContext.isNullable: Boolean
  get() = !column_constraint().any { it.K_NOT() != null }

internal val SqliteParser.Column_defContext.rawJavaType: TypeName
  get() {
    val javaTypeName = type_name().java_type_name()
    if (javaTypeName != null) {
      if (javaTypeName.K_JAVA_BOOLEAN() != null) return TypeName.BOOLEAN
      if (javaTypeName.K_JAVA_BYTE_ARRAY() != null) return ArrayTypeName.of(TypeName.BYTE)
      if (javaTypeName.K_JAVA_DOUBLE() != null) return TypeName.DOUBLE
      if (javaTypeName.K_JAVA_FLOAT() != null) return TypeName.FLOAT
      if (javaTypeName.K_JAVA_INTEGER() != null) return TypeName.INT
      if (javaTypeName.K_JAVA_LONG() != null) return TypeName.LONG
      if (javaTypeName.K_JAVA_STRING() != null) return ClassName.get(String::class.java)
      try {
        return typeForCustomClass(javaTypeName.custom_type())
      } catch (e: IllegalArgumentException) {
        throw SqlitePluginException(this,
            "Couldn't make a guess for type of column $name : '${javaTypeName.text}'")
      }
    }
    return type.defaultType
  }

private fun typeForCustomClass(customType: SqliteParser.Custom_typeContext): TypeName {
  if (customType.custom_type().isNotEmpty()) {
    return ParameterizedTypeName.get(
        ClassName.bestGuess(customType.JAVA_TYPE().text),
        *customType.custom_type().map { typeForCustomClass(it) }.toTypedArray()
    )
  }
  return ClassName.bestGuess(customType.JAVA_TYPE().text)
}

internal val SqliteParser.Column_defContext.javaType: TypeName
  get() {
    val rawJavaType = rawJavaType
    return if (isNullable) rawJavaType.box() else rawJavaType
  }

internal val SqliteParser.Column_defContext.isHandledType: Boolean
  get() = type.handledTypes.contains(javaType.box())

internal fun SqliteParser.Column_defContext.adapterType() =
    ParameterizedTypeName.get(SqliteCompiler.COLUMN_ADAPTER_TYPE, javaType.box())

internal fun SqliteParser.Column_defContext.adapterField() = adapterField(name)
internal fun SqliteParser.Column_defContext.marshaledValue() =
    if (javaType == TypeName.BOOLEAN || javaType == TypeName.BOOLEAN.box())
      "$methodName ? 1 : 0"
    else
      methodName

fun methodName(name: String) = name
fun adapterField(name: String) = name + "Adapter"
