package app.cash.sqldelight.dialects.hsql

import app.cash.sqldelight.dialect.api.DialectType
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.TypeName

internal enum class HsqlType(override val javaType: TypeName) : DialectType {
  TINY_INT(BYTE) {
    override fun decode(value: CodeBlock) = CodeBlock.of("%L.toByte()", value)

    override fun encode(value: CodeBlock) = CodeBlock.of("%L.toLong()", value)
  },
  SMALL_INT(SHORT) {
    override fun decode(value: CodeBlock) = CodeBlock.of("%L.toShort()", value)

    override fun encode(value: CodeBlock) = CodeBlock.of("%L.toLong()", value)
  },
  INTEGER(INT) {
    override fun decode(value: CodeBlock) = CodeBlock.of("%L.toInt()", value)

    override fun encode(value: CodeBlock) = CodeBlock.of("%L.toLong()", value)
  },
  BIG_INT(LONG),
  BOOL(BOOLEAN) {
    override fun decode(value: CodeBlock) = CodeBlock.of("%L == 1L", value)

    override fun encode(value: CodeBlock) = CodeBlock.of("if (%L) 1L else 0L", value)
  };

  override fun prepareStatementBinder(columnIndex: String, value: CodeBlock): CodeBlock {
    return CodeBlock.builder()
      .add(
        when (this) {
          TINY_INT, SMALL_INT, INTEGER, BIG_INT, BOOL -> "bindLong"
        }
      )
      .add("($columnIndex, %L)\n", value)
      .build()
  }

  override fun cursorGetter(columnIndex: Int, cursorName: String): CodeBlock {
    return CodeBlock.of(
      when (this) {
        TINY_INT, SMALL_INT, INTEGER, BIG_INT, BOOL -> "$cursorName.getLong($columnIndex)"
      }
    )
  }
}
