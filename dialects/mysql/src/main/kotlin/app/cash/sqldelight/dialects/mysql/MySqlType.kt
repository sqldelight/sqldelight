package app.cash.sqldelight.dialects.mysql

import app.cash.sqldelight.dialect.api.DialectType
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.TypeName

internal enum class MySqlType(override val javaType: TypeName) : DialectType {
  TINY_INT(BYTE) {
    override fun decode(value: CodeBlock) = CodeBlock.of("%L.toByte()", value)

    override fun encode(value: CodeBlock) = CodeBlock.of("%L.toLong()", value)
  },
  TINY_INT_BOOL(BOOLEAN) {
    override fun decode(value: CodeBlock) = CodeBlock.of("%L == 1L", value)

    override fun encode(value: CodeBlock) = CodeBlock.of("if (%L) 1L else 0L", value)
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
  BIT(BOOLEAN) {
    override fun decode(value: CodeBlock) = CodeBlock.of("%L == 1L", value)

    override fun encode(value: CodeBlock) = CodeBlock.of("if (%L) 1L else 0L", value)
  },
  NUMERIC(ClassName("java.math", "BigDecimal")),
  DATE(ClassName("java.time", "LocalDate")),
  TIME(ClassName("java.time", "LocalTime")),
  DATETIME(ClassName("java.time", "LocalDateTime")),
  TIMESTAMP(ClassName("java.time", "OffsetDateTime")),
  ;

  override fun prepareStatementBinder(columnIndex: CodeBlock, value: CodeBlock): CodeBlock {
    return when (this) {
      TINY_INT, TINY_INT_BOOL, SMALL_INT, INTEGER, BIG_INT, BIT -> CodeBlock.of("bindLong(%L, %L)\n", columnIndex, value)
      DATE -> CodeBlock.of("bindObject(%L, %L, %M)\n", columnIndex, value, MemberName(ClassName("java.sql", "Types"), "DATE"))
      TIME -> CodeBlock.of("bindObject(%L, %L, %M)\n", columnIndex, value, MemberName(ClassName("java.sql", "Types"), "TIME"))
      DATETIME -> CodeBlock.of("bindObject(%L, %L, %M)\n", columnIndex, value, MemberName(ClassName("java.sql", "Types"), "OTHER"))
      TIMESTAMP -> CodeBlock.of("bindObject(%L, %L, %M)\n", columnIndex, value, MemberName(ClassName("java.sql", "Types"), "TIMESTAMP_WITH_TIMEZONE"))
      NUMERIC -> CodeBlock.of("bindBigDecimal(%L, %L)\n", columnIndex, value)
    }
  }

  override fun cursorGetter(columnIndex: Int, cursorName: String): CodeBlock {
    return CodeBlock.of(
      when (this) {
        TINY_INT, TINY_INT_BOOL, SMALL_INT, INTEGER, BIG_INT, BIT -> "$cursorName.getLong($columnIndex)"
        DATE, TIME, DATETIME, TIMESTAMP -> "$cursorName.getObject<%T>($columnIndex)"
        NUMERIC -> "$cursorName.getBigDecimal($columnIndex)"
      },
      javaType,
    )
  }
}
