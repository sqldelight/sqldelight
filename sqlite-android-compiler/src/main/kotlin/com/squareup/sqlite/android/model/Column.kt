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
package com.squareup.sqlite.android.model

import com.google.common.base.CaseFormat
import com.google.common.base.CaseFormat.LOWER_UNDERSCORE
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ClassName.bestGuess
import com.squareup.javapoet.TypeName
import com.squareup.sqlite.android.SqlitePluginException
import com.squareup.sqlite.android.model.ColumnConstraint.NotNullConstraint
import java.util.ArrayList

class Column<T>(internal val name: String, val type: Type, fullyQualifiedClass: String? = null,
    originatingElement: T) : SqlElement<T>(originatingElement) {
  enum class Type internal constructor(internal val defaultType: TypeName?, val replacement: String) {
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

  private val classType: TypeName?

  internal val javaType: TypeName
    get() = when {
      type.defaultType == null && classType != null -> classType
      type.defaultType == null -> throw SqlitePluginException(originatingElement as Any,
          "Couldnt make a guess for type of colum " + name)
      notNullConstraint != null -> type.defaultType
      else -> type.defaultType.box()
    }

  val constraints: MutableList<ColumnConstraint<T>> = ArrayList()
  val isHandledType: Boolean
    get() = type != Type.CLASS
  val isNullable: Boolean
    get() = notNullConstraint == null
  val fieldName: String
    get() = fieldName(name)
  val methodName: String
    get() = methodName(name)
  val notNullConstraint: NotNullConstraint<T>?
    get() = constraints.filterIsInstance<NotNullConstraint<T>>().firstOrNull()

  init {
    var className = fullyQualifiedClass
    try {
      classType = when {
        className == null -> null
        className.startsWith("\'") -> bestGuess(className.substring(1, className.length - 1))
        else -> bestGuess(className)
      }
    } catch (ignored: IllegalArgumentException) {
      classType = null
    }
  }

  companion object {
    fun fieldName(name: String) = name.toUpperCase()
    fun methodName(name: String) =  LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name)
    fun mapperName(name: String) = LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name) + "Mapper"
    fun mapperField(name: String) = LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name) + "Mapper"
    fun marshalName(name: String) = LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name) + "Marshal"
    fun marshalField(name: String) = LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name) + "Marshal"
  }
}
