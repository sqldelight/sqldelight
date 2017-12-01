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

import com.alecstrong.sqlite.psi.core.psi.SqliteCreateTableStmt
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.sqldelight.core.compiler.TableInterfaceGenerator
import com.squareup.sqldelight.core.lang.psi.ColumnDefMixin

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
    val name: String = "value"
) {
  fun asNullable() = copy(javaType = javaType.asNullable())

  fun asNonNullable() = copy(javaType = javaType.asNonNullable())

  fun nullableIf(predicate: Boolean): IntermediateType {
    return if (predicate) asNullable() else asNonNullable()
  }

  fun cursorGetter(columnIndex: Int): CodeBlock {
    var cursorGetter = when (javaType) {
      FLOAT -> CodeBlock.of("cursor.getFloat($columnIndex)")
      SHORT -> CodeBlock.of("cursor.getShort($columnIndex)")
      INT -> CodeBlock.of("cursor.getLong($columnIndex)")
      BOOLEAN -> CodeBlock.of("cursor.getInt($columnIndex) == 1")
      else -> sqliteType.cursorGetter(columnIndex)
    }
    column?.adapter()?.let { adapter ->
      val tableName = (column.parent as SqliteCreateTableStmt).tableName.name
      cursorGetter = CodeBlock.builder()
          .add("database.$tableName${TableInterfaceGenerator.ADAPTER_NAME}.%N.decode(", adapter)
          .add(cursorGetter)
          .add(")")
          .build()
    }

    if (javaType.nullable) {
      return CodeBlock.builder()
          .add("if (cursor.isNull($columnIndex)) null else ")
          .add(cursorGetter)
          .build()
    }

    return cursorGetter
  }

  enum class SqliteType(val javaType: TypeName) {
    ARGUMENT(ANY),
    NULL(Nothing::class.asClassName().asNullable()),
    INTEGER(LONG),
    REAL(FLOAT),
    TEXT(String::class.asTypeName()),
    BLOB(ByteArray::class.asTypeName());

    fun cursorGetter(columnIndex: Int): CodeBlock {
      return when (this) {
        NULL -> CodeBlock.of("null")
        INTEGER -> CodeBlock.of("cursor.getLong($columnIndex)")
        REAL -> CodeBlock.of("cursor.getDouble($columnIndex)")
        TEXT -> CodeBlock.of("cursor.getString($columnIndex)")
        BLOB -> CodeBlock.of("cursor.getBlob($columnIndex)")
        ARGUMENT -> {
          val block = CodeBlock.Builder()
              .beginControlFlow("when (cursor.getType($columnIndex))")

          if (javaType.nullable) {
            block.addStatement("%T.FIELD_TYPE_NULL -> null", CURSOR_TYPE)
          }
          block.addStatement("%T.FIELD_TYPE_INTEGER -> ${INTEGER.cursorGetter(columnIndex)}", CURSOR_TYPE)
              .addStatement("%T.FIELD_TYPE_FLOAT -> ${REAL.cursorGetter(columnIndex)}", CURSOR_TYPE)
              .addStatement("%T.FIELD_TYPE_STRING -> ${TEXT.cursorGetter(columnIndex)}", CURSOR_TYPE)
              .addStatement("%T.FIELD_TYPE_BLOB -> ${BLOB.cursorGetter(columnIndex)}", CURSOR_TYPE)
              .endControlFlow()
              .build()
        }
      }
    }
  }

  companion object {
    private val CURSOR_TYPE = ClassName("android.database", "Cursor")
  }
}
