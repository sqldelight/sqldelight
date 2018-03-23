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

import com.alecstrong.sqlite.psi.core.psi.SqliteBindExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteCreateTableStmt
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
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
  val bindArg: SqliteBindExpr? = null
) {
  fun asNullable() = copy(javaType = javaType.asNullable())

  fun asNonNullable() = copy(javaType = javaType.asNonNullable())

  fun nullableIf(predicate: Boolean): IntermediateType {
    return if (predicate) asNullable() else asNonNullable()
  }

  fun argumentType() = if (bindArg?.isArrayParameter() == true) {
    ParameterizedTypeName.get(Collection::class.asClassName(), javaType)
  } else {
    javaType
  }

  /**
   * @return A [CodeBlock] which binds this type to [columnIndex] on [STATEMENT_NAME].
   *
   * eg: statement.bindBytes(0, queryWrapper.tableNameAdapter.columnNameAdapter.encode(column))
   */
  fun preparedStatementBinder(columnIndex: String): CodeBlock {
    val value = column?.adapter()?.let { adapter ->
      val adapterName = (column.parent as SqliteCreateTableStmt).adapterName
      CodeBlock.of("$QUERY_WRAPPER_NAME.$adapterName.%N.encode($name)", adapter)
    } ?: when (javaType.asNonNullable()) {
      FLOAT -> CodeBlock.of("$name.toDouble()")
      SHORT -> CodeBlock.of("$name.toLong()")
      INT -> CodeBlock.of("$name.toLong()")
      BOOLEAN -> CodeBlock.of("if ($name) 1L else 0L")
      else -> {
        return sqliteType.prepareStatementBinder(columnIndex, CodeBlock.of(name))
      }
    }

    if (javaType.nullable) {
      return sqliteType.prepareStatementBinder(columnIndex, CodeBlock.builder()
          .add("if ($name == null) null else ")
          .add(value)
          .build())
    }

    return sqliteType.prepareStatementBinder(columnIndex, value)
  }

  fun resultSetGetter(columnIndex: Int): CodeBlock {
    var resultSetGetter = when (javaType) {
      FLOAT -> CodeBlock.of("$RESULT_SET_NAME.getDouble($columnIndex).toFloat()")
      SHORT -> CodeBlock.of("$RESULT_SET_NAME.getLong($columnIndex).toShort()")
      INT -> CodeBlock.of("$RESULT_SET_NAME.getLong($columnIndex).toInt()")
      else -> sqliteType.resultSetGetter(columnIndex)
    }

    if (!javaType.nullable) {
      resultSetGetter = CodeBlock.of("$resultSetGetter!!")
    }

    if (javaType == BOOLEAN) {
      resultSetGetter = CodeBlock.of("$resultSetGetter == 1L")
    } else if (javaType == BOOLEAN.asNullable()) {
      resultSetGetter = CodeBlock.of("$resultSetGetter?.let { it == 1L }")
    }

    column?.adapter()?.let { adapter ->
      val adapterName = (column.parent as SqliteCreateTableStmt).adapterName
      resultSetGetter = CodeBlock.builder()
          .add("$QUERY_WRAPPER_NAME.$adapterName.%N.decode(", adapter)
          .add(resultSetGetter)
          .add(")")
          .build()
    }

    return resultSetGetter
  }

  enum class SqliteType(val javaType: TypeName) {
    ARGUMENT(ANY),
    NULL(Nothing::class.asClassName().asNullable()),
    INTEGER(LONG),
    REAL(DOUBLE),
    TEXT(String::class.asTypeName()),
    BLOB(ByteArray::class.asTypeName());

    fun prepareStatementBinder(columnIndex: String, value: CodeBlock): CodeBlock {
      return CodeBlock.builder()
          .add("$STATEMENT_NAME.")
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
        INTEGER -> "$RESULT_SET_NAME.getLong($columnIndex)"
        REAL -> "$RESULT_SET_NAME.getDouble($columnIndex)"
        TEXT -> "$RESULT_SET_NAME.getString($columnIndex)"
        ARGUMENT, BLOB -> "$RESULT_SET_NAME.getBytes($columnIndex)"
      })
    }
  }
}
