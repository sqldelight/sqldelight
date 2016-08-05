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
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.SqlitePluginException
import com.squareup.sqldelight.types.SqliteType
import org.antlr.v4.runtime.RuleContext

internal val SqliteParser.Column_defContext.type: SqliteType
  get() = SqliteType.valueOf(type_name().getChild(0).getChild(0).text)

internal val SqliteParser.Column_defContext.isNullable: Boolean
  get() = !column_constraint().any { it.K_NOT() != null }

private fun SqliteParser.Java_type_nameContext.typeForJavaTypeName(): TypeName {
  if (K_JAVA_BOOLEAN() != null) return TypeName.BOOLEAN
  if (K_JAVA_BYTE_ARRAY() != null) return ArrayTypeName.of(TypeName.BYTE)
  if (K_JAVA_DOUBLE() != null) return TypeName.DOUBLE
  if (K_JAVA_FLOAT() != null) return TypeName.FLOAT
  if (K_JAVA_SHORT() != null) return TypeName.SHORT
  if (K_JAVA_INTEGER() != null) return TypeName.INT
  if (K_JAVA_LONG() != null) return TypeName.LONG
  if (K_JAVA_STRING() != null) return ClassName.get(String::class.java)
  try {
    return custom_type().typeForCustomClass()
  } catch (e: IllegalArgumentException) {
    throw SqlitePluginException(this, "Couldn't make a guess for type '$text'")
  }
}

private fun SqliteParser.Custom_typeContext.typeForCustomClass(): TypeName {
  if (java_type_name().isNotEmpty() || java_type_name2().isNotEmpty()) {
    var parameters = java_type_name().map { it.typeForJavaTypeName().box() }.toTypedArray()
    if (java_type().size == 2) {
      // Hack to get around '>>' character.
      parameters += ParameterizedTypeName.get(
          fullyQualifiedType(java_type(1).text),
          *java_type_name2().map { it.java_type_name().typeForJavaTypeName().box() }.toTypedArray()
      )
    }
    return ParameterizedTypeName.get(fullyQualifiedType(java_type(0).text), *parameters)
  }
  return fullyQualifiedType(java_type(0).text)
}

private fun SqliteParser.Custom_typeContext.fullyQualifiedType(text: String): ClassName {
  containingParse().sql_stmt_list().import_stmt().forEach { import ->
    val typePrefix = text.substringBefore('.')
    if (import.java_type_name().text.endsWith(typePrefix)) {
      // If the text given has as a prefix the suffix of the import we're checking against
      // then that is the import we use for this type. For example:
      //   - import com.sample.User;
      // can be used for both
      //   - some_user BLOB AS User
      //   - some_enum TEXT AS User.AnEnum
      // Since the suffix of the import (User) is the same as the prefix of the type (User)

      // This operation takes the type from the import minus the shared prefix and appends
      // the full type. So in our last example the enum becomes
      //   "com.sample." + "User.AnEnum"
      return ClassName.bestGuess(import.java_type_name().text.substringBefore(typePrefix) + text)
    }
  }
  return ClassName.bestGuess(text)
}

private fun RuleContext.containingParse(): SqliteParser.ParseContext =
  when (this) {
    is SqliteParser.ParseContext -> this
    else -> this.parent.containingParse()
  }

internal val SqliteParser.Column_defContext.javaType: TypeName
  get() {
    val rawJavaType = type_name().java_type_name()?.typeForJavaTypeName() ?: type.defaultType
    return if (isNullable) rawJavaType.box() else rawJavaType
  }

fun methodName(name: String) = name

internal fun String.columnName() = trim('\'', '`', '[', ']', '"')
