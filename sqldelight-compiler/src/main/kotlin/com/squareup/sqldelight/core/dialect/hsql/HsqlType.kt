package com.squareup.sqldelight.core.dialect.hsql

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.TypeName
import com.squareup.sqldelight.core.dialect.api.DialectType
import com.squareup.sqldelight.core.lang.CURSOR_NAME

internal enum class HsqlType(override val javaType: TypeName) : DialectType {
  TINY_INT(BYTE),
  SMALL_INT(SHORT),
  INTEGER(INT),
  BIG_INT(LONG),
  BOOL(BOOLEAN);

  override fun prepareStatementBinder(columnIndex: String, value: CodeBlock): CodeBlock {
    return CodeBlock.builder()
      .add(when (this) {
        TINY_INT, SMALL_INT, INTEGER, BIG_INT, BOOL -> "bindLong"
      })
      .add("($columnIndex, %L)\n", value)
      .build()
  }

  override fun cursorGetter(columnIndex: Int): CodeBlock {
    return CodeBlock.of(when (this) {
      TINY_INT, SMALL_INT, INTEGER, BIG_INT, BOOL -> "$CURSOR_NAME.getLong($columnIndex)"
    })
  }
}
