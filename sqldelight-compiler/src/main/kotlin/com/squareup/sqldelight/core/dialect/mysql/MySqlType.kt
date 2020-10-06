package com.squareup.sqldelight.core.dialect.mysql

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.TypeName
import com.squareup.sqldelight.core.dialect.api.DialectType
import com.squareup.sqldelight.core.lang.CURSOR_NAME

internal enum class MySqlType(override val javaType: TypeName) : DialectType {
  TINY_INT(BYTE),
  TINY_INT_BOOL(BOOLEAN),
  SMALL_INT(SHORT),
  INTEGER(INT),
  BIG_INT(LONG),
  BIT(BOOLEAN);

  override fun prepareStatementBinder(columnIndex: String, value: CodeBlock): CodeBlock {
    return CodeBlock.builder()
      .add(when (this) {
        TINY_INT, TINY_INT_BOOL, SMALL_INT, INTEGER, BIG_INT, BIT -> "bindLong"
      })
      .add("($columnIndex, %L)\n", value)
      .build()
  }

  override fun cursorGetter(columnIndex: Int): CodeBlock {
    return CodeBlock.of(when (this) {
      TINY_INT, TINY_INT_BOOL, SMALL_INT, INTEGER, BIG_INT, BIT -> "$CURSOR_NAME.getLong($columnIndex)"
    })
  }
}
