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
package com.squareup.sqldelight.resolution.query

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.NameAllocator
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.sqldelight.FactorySpec
import com.squareup.sqldelight.SqliteCompiler
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.model.annotations
import com.squareup.sqldelight.model.columnName
import com.squareup.sqldelight.model.isNullable
import com.squareup.sqldelight.model.javaType
import com.squareup.sqldelight.model.type
import com.squareup.sqldelight.types.SqliteType
import com.squareup.sqldelight.util.getOrSet
import com.squareup.sqldelight.util.javadocText
import org.antlr.v4.runtime.ParserRuleContext

/**
 * Corresponds to a single result column in a SQL select. Cases:
 *
 *  1. SELECT column FROM table;
 *  2. SELECT expression FROM table;
 *
 */
data class Value private constructor(
    override val name: String,
    private val rawJavaType: TypeName,
    override val element: ParserRuleContext,
    override val nullable: Boolean,
    internal val hasDefaultValue: Boolean,
    internal val column: SqliteParser.Column_defContext?,
    internal val tableInterface: ClassName?,
    internal val dataType: SqliteType,
    private val nameAllocator: NameAllocator,
    internal val tableName: String? = null,
    internal val adapterField: String = nameAllocator.newName(name.columnName() + "Adapter")
) : Result {
  override val javaType = if (nullable) {
    rawJavaType.box()
  } else if (rawJavaType.isBoxedPrimitive) {
    rawJavaType.unbox()
  } else {
    rawJavaType
  }

  internal val isHandledType = dataType.contains(javaType)
  internal val methodName = nameAllocator.getOrSet(element, name.columnName())
  internal val constantName = SqliteCompiler.constantName(methodName)
  internal val adapterType = ParameterizedTypeName.get(SqliteCompiler.COLUMN_ADAPTER_TYPE, javaType.box(), dataType.defaultType.box())
  internal val javadocText = if (column != null) javadocText(column.JAVADOC_COMMENT()) else null
  internal val annotations = column?.annotations

  /**
   * SELECT expression FROM table;
   */
  internal constructor(
      expression: SqliteParser.ExprContext,
      dataType: SqliteType,
      nullable: Boolean,
      nameAllocator: NameAllocator = NameAllocator()
  ) : this(
      expression.methodName() ?: "expr",
      dataType.defaultType,
      expression,
      nullable,
      false,
      null,
      null,
      dataType,
      nameAllocator
  )

  /**
   * SELECT column FROM table;
   */
  internal constructor (
      column: SqliteParser.Column_defContext,
      tableInterface: TypeName,
      tableName: String,
      nameAllocator: NameAllocator = NameAllocator()
  ) : this(
      column.column_name().text,
      column.javaType,
      column.column_name(),
      column.isNullable,
      column.column_constraint().any { it.K_DEFAULT() != null || it.K_AUTOINCREMENT() != null },
      column,
      tableInterface as ClassName,
      column.type,
      nameAllocator,
      tableName
  )

  override fun tableNames() = emptyList<String>()
  override fun columnNames() = listOf(name)
  override fun size() = 1
  override fun expand() = listOf(this)
  override fun findElement(columnName: String, tableName: String?) =
    if (tableName == null && columnName == name) listOf(this) else emptyList()

  override fun merge(other: Result): Result {
    if (other !is Value) throw AssertionError()
    if (javaType == other.javaType) {
      return copy(nullable = nullable || other.nullable)
    } else if (javaType != dataType.defaultType && dataType.contains(other.javaType)) {
      // Custom type on this value and the already existing adapter can handle the other result.
      return copy(nullable = nullable || other.nullable)
    } else if (other.javaType != other.dataType.defaultType && other.dataType.contains(javaType)) {
      // Custom type on other value and we want to use the other columns adapter, but keep this
      // columns name/alias information.
      return other.copy(
          name = name,
          element = element,
          tableName = tableName,
          nullable = nullable || other.nullable
      )
    } else if (other.dataType == SqliteType.NULL) {
      return copy(nullable = true)
    } else if (dataType == SqliteType.NULL) {
      return other.copy(name = name, element = element, tableName = tableName, nullable = true)
    } else {
      // Custom type merging wont work, instead get the ceil of the two data types and use that
      // for both the data type and java type.
      val type = listOf(this, other).ceilType()
      return copy(
          dataType = type,
          rawJavaType = type.defaultType,
          nullable = nullable || other.nullable
      )
    }
  }

  internal fun marshaledValue() =
    if (javaType == TypeName.BOOLEAN || javaType == TypeName.BOOLEAN.box()) "$methodName ? 1 : 0"
    else methodName

  internal fun factoryField() = "${tableInterface!!.simpleName().decapitalize()}${FactorySpec.FACTORY_NAME}"

  companion object {
    private fun SqliteParser.ExprContext.methodName(): String? {
      if (column_name() != null) {
        if (table_name() != null) {
          return "${table_name().text}_${column_name().text}"
        }
        return column_name().text
      }
      if (literal_value() != null) {
        return literal_value().methodName()
      }
      if (function_name() != null) {
        if (expr().size == 0) {
          return function_name().text
        }
        return "${function_name().text}_${expr(0).methodName() ?: return function_name().text}"
      }
      return null
    }

    private fun SqliteParser.Literal_valueContext.methodName(): String {
      if (INTEGER_LITERAL() != null) {
        return "int_literal"
      }
      if (REAL_LITERAL() != null) {
        return "real_literal"
      }
      if (STRING_LITERAL() != null) {
        return "string_literal"
      }
      if (BLOB_LITERAL() != null) {
        return "blob_literal"
      }
      return "literal"
    }
  }
}
