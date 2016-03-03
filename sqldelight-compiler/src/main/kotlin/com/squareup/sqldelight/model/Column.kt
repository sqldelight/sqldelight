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
import com.squareup.sqldelight.model.Type.BLOB
import com.squareup.sqldelight.model.Type.BOOLEAN
import com.squareup.sqldelight.model.Type.DOUBLE
import com.squareup.sqldelight.model.Type.ENUM
import com.squareup.sqldelight.model.Type.FLOAT
import com.squareup.sqldelight.model.Type.INT
import com.squareup.sqldelight.model.Type.LONG
import com.squareup.sqldelight.model.Type.SHORT
import com.squareup.sqldelight.model.Type.STRING

internal enum class Type constructor(internal val defaultType: TypeName?, val replacement: String) {
  INT(TypeName.INT, "INTEGER"),
  LONG(TypeName.LONG, "INTEGER"),
  SHORT(TypeName.SHORT, "INTEGER"),
  DOUBLE(TypeName.DOUBLE, "REAL"),
  FLOAT(TypeName.FLOAT, "REAL"),
  BOOLEAN(TypeName.BOOLEAN, "INTEGER"),
  STRING(ClassName.get(String::class.java), "TEXT"),
  BLOB(ArrayTypeName.of(TypeName.BYTE), "BLOB"),
  ENUM(null, "TEXT"),
  CLASS(null, "BLOB")
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

private val SqliteParser.Column_defContext.sqliteClassName: String
  get() = type_name().sqlite_class_name().STRING_LITERAL().text.filter { it != '\'' }

internal val SqliteParser.Column_defContext.javaType: TypeName
  get() = try {
    when {
      type_name().sqlite_class_name() != null -> ClassName.bestGuess(sqliteClassName)
      isNullable -> type.defaultType!!.box()
      else -> type.defaultType!!
    }
  } catch (e: IllegalArgumentException) {
    throw SqlitePluginException(this,
        "Couldn't make a guess for type of column $name : '$sqliteClassName'")
  }

internal val SqliteParser.Column_defContext.isHandledType: Boolean
  get() = type != Type.CLASS

internal fun SqliteParser.Column_defContext.adapterType() =
    ParameterizedTypeName.get(SqliteCompiler.COLUMN_ADAPTER_TYPE, javaType)

internal fun SqliteParser.Column_defContext.adapterField() = adapterField(name)
internal fun SqliteParser.Column_defContext.marshaledValue() =
    when (type) {
      INT, LONG, SHORT, DOUBLE, FLOAT,
      STRING, BLOB -> methodName
      BOOLEAN -> "$methodName ? 1 : 0"
      ENUM -> "$methodName.name()"
      else -> throw IllegalStateException("Unexpected type")
    }

fun methodName(name: String) = name
fun adapterField(name: String) = name + "Adapter"
