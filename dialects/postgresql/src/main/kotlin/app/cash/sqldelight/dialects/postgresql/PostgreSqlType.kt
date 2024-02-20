package app.cash.sqldelight.dialects.postgresql

import app.cash.sqldelight.dialect.api.DialectType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName

internal enum class PostgreSqlType(override val javaType: TypeName) : DialectType {
  SMALL_INT(SHORT),
  INTEGER(INT),
  BIG_INT(LONG),
  DATE(ClassName("java.time", "LocalDate")),
  TIME(ClassName("java.time", "LocalTime")),
  TIMESTAMP(ClassName("java.time", "LocalDateTime")),
  TIMESTAMP_TIMEZONE(ClassName("java.time", "OffsetDateTime")),
  INTERVAL(ClassName("org.postgresql.util", "PGInterval")),
  UUID(ClassName("java.util", "UUID")),
  NUMERIC(ClassName("java.math", "BigDecimal")),
  JSON(STRING),
  ;

  override fun prepareStatementBinder(columnIndex: CodeBlock, value: CodeBlock): CodeBlock {
    return CodeBlock.builder()
      .add(
        when (this) {
          SMALL_INT -> "bindShort"
          INTEGER -> "bindInt"
          BIG_INT -> "bindLong"
          DATE, TIME, TIMESTAMP, TIMESTAMP_TIMEZONE, INTERVAL, UUID -> "bindObject"
          NUMERIC -> "bindBigDecimal"
          JSON -> "bindObjectOther"
        },
      )
      .add("(%L, %L)\n", columnIndex, value)
      .build()
  }

  override fun cursorGetter(columnIndex: Int, cursorName: String): CodeBlock {
    return CodeBlock.of(
      when (this) {
        SMALL_INT -> "$cursorName.getShort($columnIndex)"
        INTEGER -> "$cursorName.getInt($columnIndex)"
        BIG_INT -> "$cursorName.getLong($columnIndex)"
        DATE, TIME, TIMESTAMP, TIMESTAMP_TIMEZONE, INTERVAL, UUID -> "$cursorName.getObject<%T>($columnIndex)"
        NUMERIC -> "$cursorName.getBigDecimal($columnIndex)"
        JSON -> "$cursorName.getString($columnIndex)"
      },
      javaType,
    )
  }
}
