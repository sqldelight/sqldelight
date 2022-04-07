package app.cash.sqldelight.dialects.postgresql

import app.cash.sqldelight.dialect.api.DialectType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.TypeName

internal enum class PostgreSqlType(override val javaType: TypeName) : DialectType {
  SMALL_INT(SHORT) {
    override fun decode(value: CodeBlock) = CodeBlock.of("%L.toShort()", value)

    override fun encode(value: CodeBlock) = CodeBlock.of("%L.toLong()", value)
  },
  INTEGER(INT) {
    override fun decode(value: CodeBlock) = CodeBlock.of("%L.toInt()", value)

    override fun encode(value: CodeBlock) = CodeBlock.of("%L.toLong()", value)
  },
  BIG_INT(LONG),
  DATE(ClassName("java.time", "LocalDate")),
  TIME(ClassName("java.time", "LocalTime")),
  TIMESTAMP(ClassName("java.time", "LocalDateTime")),
  TIMESTAMP_TIMEZONE(ClassName("java.time", "OffsetDateTime")),
  INTERVAL(ClassName("org.postgresql.util", "PGInterval")),
  UUID(ClassName("java.util", "UUID")),
  ;

  override fun prepareStatementBinder(columnIndex: String, value: CodeBlock): CodeBlock {
    return CodeBlock.builder()
      .add(
        when (this) {
          SMALL_INT, INTEGER, BIG_INT -> "bindLong"
          DATE, TIME, TIMESTAMP, TIMESTAMP_TIMEZONE, INTERVAL, UUID -> "bindObject"
        }
      )
      .add("($columnIndex, %L)\n", value)
      .build()
  }

  override fun cursorGetter(columnIndex: Int, cursorName: String): CodeBlock {
    return CodeBlock.of(
      when (this) {
        SMALL_INT, INTEGER, BIG_INT -> "$cursorName.getLong($columnIndex)"
        DATE, TIME, TIMESTAMP, TIMESTAMP_TIMEZONE, INTERVAL, UUID -> "$cursorName.getObject($columnIndex)"
      }
    )
  }
}
