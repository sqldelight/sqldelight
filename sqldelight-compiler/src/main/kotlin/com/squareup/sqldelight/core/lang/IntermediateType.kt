/*
 * Copyright (C) 2017 Square, Inc.
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
package com.squareup.sqldelight.core.lang

import com.alecstrong.sql.psi.core.psi.Queryable
import com.alecstrong.sql.psi.core.psi.SqlBindExpr
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.sqldelight.core.compiler.integration.adapterName
import com.squareup.sqldelight.core.lang.psi.ColumnDefMixin
import com.squareup.sqldelight.core.lang.util.isArrayParameter

/**
 * Internal representation for a column type, which has SQLite data affinity as well as JVM class
 * type.
 */
internal data class IntermediateType(
  val sqliteType: SqliteType,
  val javaType: TypeName = sqliteType.javaType,
  /**
   * The column definition this type is sourced from, or null if there is none.
   */
  val column: ColumnDefMixin? = null,
  /**
   * The name of this intermediate type as exposed in the generated api.
   */
  val name: String = "value",
  /**
   * The original bind argument expression this intermediate type comes from.
   */
  val bindArg: SqlBindExpr? = null,
  /**
   * Whether or not this argument is extracted from a different type
   */
  val extracted: Boolean = false
) {
  fun asNullable() = copy(javaType = javaType.copy(nullable = true))

  fun asNonNullable() = copy(javaType = javaType.copy(nullable = false))

  fun nullableIf(predicate: Boolean): IntermediateType {
    return if (predicate) asNullable() else asNonNullable()
  }

  fun argumentType() = if (bindArg?.isArrayParameter() == true) {
    Collection::class.asClassName().parameterizedBy(javaType)
  } else {
    javaType
  }

  /**
   * @return A [CodeBlock] which binds this type to [columnIndex] on [STATEMENT_NAME].
   *
   * eg: statement.bindBytes(0, queryWrapper.tableNameAdapter.columnNameAdapter.encode(column))
   */
  fun preparedStatementBinder(
    columnIndex: String
  ): CodeBlock {
    val value = column?.adapter()?.let { adapter ->
      val adapterName = (column.parent as Queryable).tableExposed().adapterName
      CodeBlock.of("$CUSTOM_DATABASE_NAME.$adapterName.%N.encode($name)", adapter)
    } ?: when (javaType.copy(nullable = false)) {
      FLOAT -> CodeBlock.of("$name.toDouble()")
      BYTE -> CodeBlock.of("$name.toLong()")
      SHORT -> CodeBlock.of("$name.toLong()")
      INT -> CodeBlock.of("$name.toLong()")
      BOOLEAN -> CodeBlock.of("if ($name) 1L else 0L")
      else -> {
        return sqliteType.prepareStatementBinder(columnIndex, CodeBlock.of(this.name))
      }
    }

    if (javaType.isNullable) {
      return sqliteType.prepareStatementBinder(columnIndex, CodeBlock.builder()
          .add("if (${this.name} == null) null else ")
          .add(value)
          .build())
    }

    return sqliteType.prepareStatementBinder(columnIndex, value)
  }

  fun resultSetGetter(columnIndex: Int): CodeBlock {
    var resultSetGetter = sqliteType.resultSetGetter(columnIndex)

    if (!javaType.isNullable) {
      resultSetGetter = CodeBlock.of("$resultSetGetter!!")
    }

    resultSetGetter = when (javaType) {
      FLOAT -> CodeBlock.of("$resultSetGetter.toFloat()")
      FLOAT.copy(nullable = true) -> CodeBlock.of("$resultSetGetter?.toFloat()")
      BYTE -> CodeBlock.of("$resultSetGetter.toByte()")
      BYTE.copy(nullable = true) -> CodeBlock.of("$resultSetGetter?.toByte()")
      SHORT -> CodeBlock.of("$resultSetGetter.toShort()")
      SHORT.copy(nullable = true) -> CodeBlock.of("$resultSetGetter?.toShort()")
      INT -> CodeBlock.of("$resultSetGetter.toInt()")
      INT.copy(nullable = true) -> CodeBlock.of("$resultSetGetter?.toInt()")
      BOOLEAN -> CodeBlock.of("$resultSetGetter == 1L")
      BOOLEAN.copy(nullable = true) -> CodeBlock.of("$resultSetGetter?.let { it == 1L }")
      else -> resultSetGetter
    }

    column?.adapter()?.let { adapter ->
      val adapterName = (column.parent as Queryable).tableExposed().adapterName
      resultSetGetter = if (javaType.isNullable) {
        CodeBlock.of("%L?.let($CUSTOM_DATABASE_NAME.$adapterName.%N::decode)", resultSetGetter, adapter)
      } else {
        CodeBlock.of("$CUSTOM_DATABASE_NAME.$adapterName.%N.decode(%L)", adapter, resultSetGetter)
      }
    }

    return resultSetGetter
  }

  enum class SqliteType(val javaType: TypeName) {
    ARGUMENT(ANY.copy(nullable = true)),
    NULL(Nothing::class.asClassName().copy(nullable = true)),
    INTEGER(LONG),
    REAL(DOUBLE),
    TEXT(String::class.asTypeName()),
    BLOB(ByteArray::class.asTypeName());

    fun prepareStatementBinder(columnIndex: String, value: CodeBlock): CodeBlock {
      return CodeBlock.builder()
          .add(when (this) {
            INTEGER -> "bindLong"
            REAL -> "bindDouble"
            TEXT -> "bindString"
            BLOB -> "bindBytes"
            else -> throw AssertionError("Cannot bind unknown types or null")
          })
          .add("($columnIndex, %L)\n", value)
          .build()
    }

    fun resultSetGetter(columnIndex: Int): CodeBlock {
      return CodeBlock.of(when (this) {
        NULL -> "null"
        INTEGER -> "$CURSOR_NAME.getLong($columnIndex)"
        REAL -> "$CURSOR_NAME.getDouble($columnIndex)"
        TEXT -> "$CURSOR_NAME.getString($columnIndex)"
        ARGUMENT, BLOB -> "$CURSOR_NAME.getBytes($columnIndex)"
      })
    }
  }
}
